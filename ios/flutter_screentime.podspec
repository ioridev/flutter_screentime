Pod::Spec.new do |s|
  s.name             = 'flutter_screentime'
  s.version          = '0.1.0'
  s.summary          = 'Flutter plugin for Screen Time style blocking.'
  s.description      = <<-DESC
Flutter plugin for Screen Time style blocking with host-configured iOS extensions and configurable Android overlays.
                       DESC
  s.homepage         = 'https://github.com/ioridev/flutter_screentime'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'iori' => 'noreply@example.com' }
  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform         = :ios, '16.0'
  s.swift_version    = '5.0'
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386'
  }
end
