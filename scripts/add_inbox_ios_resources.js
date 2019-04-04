require('react-native/setupBabel')();

const core = require('react-native/local-cli/core');
const PbxFile = require('xcode/lib/pbxFile');
const path = require('path');
const xcode = require('xcode');
const fs = require('fs');
const createGroupWithMessage = require('react-native/local-cli/link/ios/createGroupWithMessage');

function pwAddInboxiOSResources() {
    core.configPromise.then(function(config) {
        const iOSconfig = config.getProjectConfig().ios;
    const pw_iOSconfig = config.getDependencyConfig('pushwoosh-react-native-plugin').ios;

    const project = xcode.project(iOSconfig.pbxprojPath).parseSync();

    var targets = project.pbxNativeTargetSection();

    for (uuid in targets) {
        var libFiles = project.pbxFrameworksBuildPhaseObj(uuid).files;
        var filesCount = libFiles.length;
        for (var f = 0; f < filesCount; ++f) {
            var fileRef = project.pbxBuildFileSection()[libFiles[f].value].fileRef;
            var file = project.pbxFileReferenceSection()[fileRef];
            if (file != null) {
                if (file.path == 'libPushwooshPlugin.a') {
                    createGroupWithMessage(project, 'Resources');
                    project.addResourceFile(path.relative(iOSconfig.sourceDir, path.join(pw_iOSconfig.sourceDir, 'PushwooshInboxBundle.bundle')), {'target' : uuid }, null);
                }
            }
        }
    }
    fs.writeFileSync(
        iOSconfig.pbxprojPath,
        project.writeSync()
      );
    });

    
};

pwAddInboxiOSResources();
