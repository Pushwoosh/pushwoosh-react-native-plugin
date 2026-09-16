Pod::Spec.new do |s|
  s.name             = "pushwoosh-react-native-plugin"
  s.version          = "7.0.1"
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

  s.dependency 'PushwooshXCFramework', '7.2.6'
  s.dependency 'PushwooshInboxUIXCFramework', '7.0.42'
end
