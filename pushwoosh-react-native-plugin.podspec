Pod::Spec.new do |s|
  s.name             = "pushwoosh-react-native-plugin"
  s.version          = "5.6.3"
  s.summary          = "React Native Pushwoosh Push Notifications module"
  s.requires_arc = true
  s.author       = 'Pushwoosh'
  s.license      = 'MIT'
  s.homepage     = 'n/a'
  s.source       = { :git => "https://github.com/Pushwoosh/pushwoosh-react-native-plugin.git" }
  s.source_files = 'src/ios/**/*'
  s.platform     = :ios, "7.0"

end