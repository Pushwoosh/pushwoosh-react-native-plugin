# Podfile helper for React Native 0.75+: moves the app to the Pushwoosh Swift package and links it.
#
# Loading this file is the opt-in: the plugin's podspec takes the native SDK through
# `spm_dependency` only when `pushwoosh_spm_post_install` is defined by the time CocoaPods evaluates
# it, so the `require` belongs at the top level of the Podfile; without it the podspec stays on the
# CocoaPods trunk pods. `spm_dependency` is what React Native resolves
# into the Pods project only. The plugin compiles against the package there, but the package's
# products are dynamic frameworks: the app target links them, Xcode embeds the package products
# a target links, and a notification service extension has to link them too. This helper does
# that in post_install:
#
#   require_relative '../node_modules/pushwoosh-react-native-plugin/scripts/pushwoosh-spm'
#
#   post_install do |installer|
#     react_native_post_install(installer, config[:reactNativePath], :mac_catalyst_enabled => false)
#     pushwoosh_spm_post_install(installer, extra_targets: ['NotificationService'])
#   end
#
# It runs after react_native_post_install because that is where React Native records the package
# in the Pods project; the package version is read from there, so the podspec stays the only
# place that pins it. On React Native below 0.75 there is no package and the helper does nothing.
# A second pod install changes nothing: the reference and the products are added once.
require 'xcodeproj'

module PushwooshSPM
  PACKAGE_URL = 'https://github.com/Pushwoosh/Pushwoosh-XCFramework.git'.freeze
  PLUGIN_POD = 'pushwoosh-react-native-plugin'.freeze
  PRODUCTS = %w[PushwooshFramework PushwooshCore PushwooshBridge].freeze
  TRUNK_PODS = %w[PushwooshXCFramework PushwooshInboxUIXCFramework].freeze

  module_function

  def post_install(installer, extra_targets: [])
    package = installer.pods_project && find_package(installer.pods_project)
    unless package
      log 'no Pushwoosh package in the Pods project (React Native below 0.75, or the helper ran ' \
          'before react_native_post_install): nothing to do'
      return
    end
    requirement = checked_requirement(package.requirement)
    warn_about_trunk_pods(installer)
    targets_by_project(installer, extra_targets).each do |project, targets|
      reference = ensure_package(project, requirement)
      targets.each { |target| ensure_products(project, target, reference) }
      project.save
    end
  end

  def find_package(project)
    find_packages(project).first
  end

  def find_packages(project)
    project.root_object.package_references.select do |reference|
      reference.is_a?(Xcodeproj::Project::Object::XCRemoteSwiftPackageReference) &&
        same_repository?(reference.repositoryURL)
    end
  end

  # A reference added by hand may spell the same repository without `.git`, with a trailing
  # slash or in another case; all of them are the package, not a second one.
  def same_repository?(url)
    normalize_url(url) == normalize_url(PACKAGE_URL)
  end

  def normalize_url(url)
    url.to_s.downcase.sub(%r{/+\z}, '').sub(/\.git\z/, '')
  end

  # The user targets of every Pods target that links the plugin, plus extra_targets by name.
  def targets_by_project(installer, extra_targets)
    projects = installer.aggregate_targets.map(&:user_project).compact.uniq
    result = Hash.new { |hash, project| hash[project] = [] }
    installer.aggregate_targets.each do |aggregate|
      next unless aggregate.pod_targets.any? { |pod| pod.pod_name == PLUGIN_POD }
      aggregate.user_targets.each { |target| add_target(result, aggregate.user_project, target) }
    end
    extra_targets.each do |name|
      target = projects.flat_map(&:targets).find { |candidate| candidate.name == name }
      unless target
        known = projects.flat_map(&:targets).map(&:name).join(', ')
        raise "pushwoosh_spm_post_install: no target named '#{name}' (extra_targets); " \
              "the project has: #{known}"
      end
      add_target(result, target.project, target)
    end
    result
  end

  def add_target(result, project, target)
    result[project] << target unless result[project].include?(target)
  end

  # A hand-edited project can carry a reference without a requirement, and the nil used to reach
  # requirement['kind'] as a NoMethodError in the middle of pod install.
  def checked_requirement(requirement)
    unless requirement.is_a?(Hash) && requirement['kind']
      raise 'pushwoosh_spm_post_install: the Pushwoosh package in the Pods project has no version ' \
            "requirement (#{requirement.inspect}). The plugin's podspec pins it through " \
            'spm_dependency, so re-run pod install on an unmodified Pods project.'
    end
    requirement
  end

  def describe(requirement)
    "#{requirement['kind']} #{requirement['version'] || requirement['minimumVersion']}".strip
  end

  def ensure_package(project, requirement)
    reference = find_package(project)
    if reference.nil?
      reference = project.new(Xcodeproj::Project::Object::XCRemoteSwiftPackageReference)
      reference.repositoryURL = PACKAGE_URL
      reference.requirement = requirement.dup
      project.root_object.package_references << reference
      log "adding #{PACKAGE_URL} (#{describe(requirement)}) to #{File.basename(project.path)}"
    else
      if reference.repositoryURL != PACKAGE_URL
        log "rewriting #{reference.repositoryURL} to #{PACKAGE_URL} in #{File.basename(project.path)}"
        reference.repositoryURL = PACKAGE_URL
      end
      if reference.requirement != requirement
        log "aligning the package requirement in #{File.basename(project.path)} with the podspec " \
            "(#{describe(requirement)})"
        reference.requirement = requirement.dup
      end
    end
    drop_duplicate_packages(project, reference)
    reference
  end

  # Adding the package by hand and then letting the helper run leaves two references to the same
  # repository, and Xcode refuses to resolve a project that declares one package identity twice.
  def drop_duplicate_packages(project, keep)
    duplicates = find_packages(project).reject { |reference| reference == keep }
    return if duplicates.empty?
    log "removing #{duplicates.size} duplicate reference(s) to #{PACKAGE_URL} in " \
        "#{File.basename(project.path)}"
    project.targets.each { |target| repoint_products(target, keep, duplicates) }
    duplicates.each do |reference|
      project.root_object.package_references.delete(reference)
      reference.remove_from_project
    end
  end

  def repoint_products(target, keep, duplicates)
    target.package_product_dependencies.to_a.each do |dependency|
      next unless duplicates.include?(dependency.package)
      twin = target.package_product_dependencies.find do |candidate|
        candidate != dependency && candidate.product_name == dependency.product_name &&
          candidate.package == keep
      end
      if twin
        drop_product(target, dependency)
      else
        dependency.package = keep
      end
    end
  end

  def drop_product(target, dependency)
    phase = target.frameworks_build_phase
    phase.files.to_a.each { |file| phase.files.delete(file) if file.product_ref == dependency }
    target.package_product_dependencies.delete(dependency)
    dependency.remove_from_project
  end

  def ensure_products(project, target, reference)
    PRODUCTS.each do |name|
      dependency = target.package_product_dependencies.find { |candidate| candidate.product_name == name }
      unless dependency
        dependency = project.new(Xcodeproj::Project::Object::XCSwiftPackageProductDependency)
        dependency.product_name = name
        target.package_product_dependencies << dependency
        log "adding #{name} to #{target.name}"
      end
      dependency.package = reference
      phase = target.frameworks_build_phase
      next if phase.files.any? { |file| file.product_ref == dependency }
      file = project.new(Xcodeproj::Project::Object::PBXBuildFile)
      file.product_ref = dependency
      phase.files << file
    end
  end

  def warn_about_trunk_pods(installer)
    found = installer.pod_targets.map(&:pod_name).uniq & TRUNK_PODS
    return if found.empty?
    log_warning "#{found.join(', ')} from CocoaPods trunk is installed next to the Swift package; " \
                'remove the pod line(s) from the Podfile. Left in an app target the build fails ' \
                "with 'Multiple commands produce ... PushwooshFramework.framework'; left in an " \
                'extension target it builds, but that target keeps the frozen trunk version ' \
                'instead of the one the package pins'
  end

  def log(message)
    line = "[Pushwoosh SPM] #{message}"
    defined?(Pod::UI) ? Pod::UI.puts(line) : puts(line)
  end

  # Pod::UI.warn is replayed in the [!] block at the end of pod install, where it is not lost.
  def log_warning(message)
    line = "[Pushwoosh SPM] WARNING: #{message}"
    defined?(Pod::UI) ? Pod::UI.warn(line) : warn(line)
  end
end

def pushwoosh_spm_post_install(installer, extra_targets: [])
  PushwooshSPM.post_install(installer, extra_targets: extra_targets)
end
