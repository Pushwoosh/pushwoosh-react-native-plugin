Pod::Spec.new do |s|
  s.name             = "pushwoosh-react-native-plugin"
  s.version          = "6.1.39"
  s.summary          = "React Native Pushwoosh Push Notifications module"
  s.requires_arc = true
  s.author       = 'Pushwoosh'
  s.license      = 'MIT'
  s.homepage     = 'n/a'
  s.source       = { :git => "https://github.com/Pushwoosh/pushwoosh-react-native-plugin.git" }
  s.source_files = 'src/ios/PushwooshPlugin/Pushwoosh.{h,m}', 'src/ios/PushwooshPlugin/PWEventDispatcher.{h,m}', 'src/ios/PushwooshPlugin/PWInlineInAppManager.{h,m}'
  s.platform     = :ios, "11.0"
  s.xcconfig = {
    "HEADER_SEARCH_PATHS" => "${PODS_ROOT}/Headers/Public/React"
  }
  s.static_framework = true

  s.dependency 'React'
  s.dependency 'PushwooshXCFramework', '6.8.6'
  s.dependency 'PushwooshInboxUIXCFramework'
end
