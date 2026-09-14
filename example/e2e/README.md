# End-to-end flows

[Maestro](https://maestro.mobile.dev) flows that drive the demo apps against the real Pushwoosh
SDK, once per way the plugin can be built: React Native 0.74 as a legacy bridge module, React
Native 0.74 with the New Architecture switched on, and React Native 0.87 (New Architecture only).

```bash
make e2e-ios        # from the repository root
make e2e-android
```

Each runner builds the sample in Release, so the JS bundle is embedded and Metro is not involved,
installs it on a simulator or emulator and runs the flows one invocation at a time, in filename
order. `E2E_ROWS="legacy-new demoapp"` limits the matrix; `E2E_IOS_SIMULATOR`,
`E2E_ANDROID_SERIAL` and `E2E_ANDROID_AVD` pick the device. Build logs and the screenshots and
view hierarchies Maestro captures for a failed flow land under `output/e2e-ios` / `output/e2e-android`.

## What the flows cover

| Flow | Checks |
| --- | --- |
| `common/01-smoke` | the app starts, the SDK is initialised and returns a HWID |
| `common/02-user-id` | `setUserId` reaches the server, `getUserId` reads it back |
| `common/03-tags` | `setTags` reaches the server, `getTags` returns the tag |
| `common/04-rich-media-style` | the Rich Media style survives a relaunch and reads back |
| `common/05-deep-link` | a `pwdemo://` link lands on the Deep Link screen with its parameters |
| `android/01-local-notification` | a notification arrives as `pushReceived` and, tapped in the shade, as `pushOpened` |
| `ios/01-push-registration` | `register()` calls back (a token on a device, the missing entitlement on a simulator) and empties the notification centre |
| `ios/02-push-foreground` | a push delivered to the running app is reported as `pushReceived`, and tapping it as `pushOpened` |
| `ios/03-push-deep-link-warm` | the same, and the push's link lands on the Deep Link screen |
| `ios/04-push-deep-link-cold` | the same push, tapped while the app is not running, routes through `Linking.getInitialURL()` |

iOS pushes come from `payloads/*.apns` through `simctl push`; the runner rewrites `__HASH__` per
delivery because the plugin ignores a repeat of the last push hash. `simctl push` needs the
notification permission, which is why the registration flow runs first. Nothing races a banner:
the runner delivers the payload between two flows, the next flow reads the event log the plugin
filled and taps the notification in the notification centre, which the registration flow emptied
so that a single tap opens it instead of expanding a group.

Android pushes are local notifications: an emulator cannot receive FCM. A simulator build is
unsigned and has no `aps-environment` entitlement, so `register()` reports that error there
instead of a push token - `simctl push` bypasses APNs and works regardless.

## When to run these

By hand, before a release - not in the pipeline and not on every change. They want a simulator and
an emulator to themselves and take about 45 minutes over the three rows, which is why
`.gitlab-ci.yml` runs the unit layers on every push and leaves these out. `E2E_ROWS` cuts the
matrix down when you only care about one row, and the `demoapp` row is skipped on a machine below
Xcode 16.1, which is what React Native 0.87 needs to configure its pods.

A machine that runs them needs Maestro, and for Android an AVD and
`android/app/google-services.json` (or `GOOGLE_SERVICES_JSON` in the environment).

### Getting the demoapp row to build on iOS

The Xcode floor is only half the story - installing that Xcode is not enough, and the failure it
produces names neither the cause nor the fix:

- Xcode 16 ships slim: the iOS platform is a separate download, so a fresh install builds nothing
  for the simulator. `xcodebuild` answers `iOS <version> is not installed` and lists no
  destinations at all, which also turns `-sdk iphonesimulator` into `Found no destinations for the
  scheme`.
- `xcodebuild -downloadPlatform iOS` without a version does not necessarily fix it: it may install
  a simulator runtime the project does not ask for. Pin the one you need:
  `sudo xcodebuild -downloadPlatform iOS -buildVersion 18.2`.
- That download takes about 45 minutes and looks like a hang: `xcodebuild` sits at 0% CPU while
  `nsurlsessiond` fetches and `STExtractionService` unpacks. Watch those two, not xcodebuild.
- Pick the Xcode version against the machine's macOS, not just against the 16.1 floor: per
  xcodereleases.com, Xcode 16.1 and 16.2 need macOS 14.5+, while 16.3 and 16.4 need 15.2 and 15.3.
  On macOS 14.6 the newest usable one is 16.2.

Opening an iOS notification by hand: a plain tap on the card may only expand it, and the gesture
that actually opens the app is a swipe right, which reveals "Open". It looks exactly like delivery
being broken when it is not - the push is there, the app just never comes up. Worth knowing when a
`pushOpened` step fails on a simulator: `common/lib/tap-notification-in-centre.yaml` taps, so a
failure there is about the gesture, not about the plugin.

## Selectors

The flows address the demo through its `testID`s, never through wording:

- `tab-actions`, `tab-settings`, `tab-deep-link` — the tab bar
- `result`, `event-log-empty`, `event-0`… — the panel on the Actions screen
- `<action>`, `<action>-input`, `<action>-input-2` — a button and its text fields, e.g. `set-tags`
- `register-switch`, `server-communication-switch`, `modal-rich-media-switch`,
  `create-local-notification`, `clear-notification-center`, `settings-result` — the Settings screen
- `deep-link-url`, `deep-link-empty`, `deep-link-<parameter>` — the Deep Link screen

A Pushwoosh project may greet the app with an In-App message; `common/lib/dismiss-in-app.yaml`
closes it where one can appear.
