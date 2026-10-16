Pod::Spec.new do |s|
  s.name             = "pushwoosh-react-native-plugin"
  s.version          = "7.0.6"
  s.summary          = "React Native Pushwoosh Push Notifications module"
  s.requires_arc = true
  s.author       = 'Pushwoosh'
  s.license      = 'MIT'
  s.homepage     = 'n/a'
  s.source       = { :git => "https://github.com/Pushwoosh/pushwoosh-react-native-plugin.git" }
  s.source_files = 'src/ios/PushwooshPlugin/Pushwoosh.{h,mm}', 'src/ios/PushwooshPlugin/PWEventDispatcher.{h,m}'
  s.platform     = :ios, "13.0"
  s.static_framework = true

  # React Native 0.71+ adds React-Core and, when the New Architecture is on, the codegen and
  # TurboModule dependencies together with the RCT_NEW_ARCH_ENABLED define.
  if respond_to?(:install_modules_dependencies, true)
    install_modules_dependencies(s)
  else
    s.dependency "React-Core"
  end

  # Podspecs are evaluated after the Podfile, so the helper is defined here only if the Podfile loads it.
  spm_available = respond_to?(:spm_dependency, true)
  if spm_available && defined?(pushwoosh_spm_post_install)
    spm_dependency(s,
      url: 'https://github.com/Pushwoosh/Pushwoosh-XCFramework.git',
      requirement: { 'kind' => 'exactVersion', 'version' => '7.2.10' },
      products: ['PushwooshFramework', 'PushwooshCore', 'PushwooshBridge'])
    # The package has no Inbox UI with the same API, so its last trunk build (7.0.42) ships with the plugin.
    s.vendored_frameworks = 'src/ios/PushwooshInboxUI.xcframework'
    s.resources = 'src/ios/PushwooshInboxBundle.bundle'
  else
    if spm_available && defined?(Pod::UI)
      message = 'pushwoosh-react-native-plugin: CocoaPods trunk becomes read-only on December 2, 2026, ' \
                'and Pushwoosh moves the iOS SDK to Swift Package Manager. Plugin releases after that date ' \
                'will use the Swift package only, the CocoaPods pods will be turned off. Switch now, it ' \
                'takes a few lines in the Podfile: ' \
                'https://github.com/Pushwoosh/pushwoosh-react-native-plugin#moving-to-the-swift-package'
      # CocoaPods evaluates this podspec several times per install.
      queued = Pod::UI.respond_to?(:warnings) && Pod::UI.warnings.any? { |w| w[:message] == message }
      Pod::UI.warn message unless queued
    end
    s.dependency 'PushwooshXCFramework', '7.2.8'
    s.dependency 'PushwooshInboxUIXCFramework', '7.0.42'
  end
end
