var fs = require("fs");
var path = require("path");

function readBuildGradle(target) {
  return fs.readFileSync(target, "utf-8");
}

function addDependencies(buildGradle) {
  var match = buildGradle.match(/^(\s*)classpath 'com.android.tools.build(.*)/m);
  var whitespace = match[1];
  
  var googlePlayDependency = whitespace + 'classpath \'com.google.gms:google-services:4.1.0\' // google-services dependency from pushwoosh-plugin';
  var modifiedLine = match[0] + '\n' + googlePlayDependency;
  
  return buildGradle.replace(/^(\s*)classpath 'com.android.tools.build(.*)/m, modifiedLine);
}
function addPlugin(buildGradle) {
  return buildGradle + "\napply plugin: \'com.google.gms.google-services\' // Google\'s Maven repository from pushwoosh-plugin";
}

function addRepos(buildGradle) {
  var match = buildGradle.match(/^(\s*)jcenter\(\)/m);
  var whitespace = match[1];

  var allProjectsIndex = buildGradle.indexOf('allprojects');
  if (allProjectsIndex > 0) {
    var firstHalfOfFile = buildGradle.substring(0, allProjectsIndex);
    var secondHalfOfFile = buildGradle.substring(allProjectsIndex);

    match = secondHalfOfFile.match(/^(\s*)jcenter\(\)/m);
    var googlesMavenRepo = whitespace + 'google() // Google\'s Maven repository from pushwoosh-plugin';
    modifiedLine = match[0] + '\n' + googlesMavenRepo;

    secondHalfOfFile = secondHalfOfFile.replace(/^(\s*)jcenter\(\)/m, modifiedLine);

    buildGradle = firstHalfOfFile + secondHalfOfFile;
  } else {
    match = buildGradle.match(/^(\s*)jcenter\(\)/m);
    var googlesMavenRepo = whitespace + 'google()';
    modifiedLine = match[0] + '\n' + googlesMavenRepo;

    buildGradle = buildGradle.replace(/^(\s*)jcenter\(\)/m, modifiedLine);
  }

  return buildGradle;
}

function writeBuildGradle(contents, target) {
  fs.writeFileSync(target, contents);
}

function restore(target){
    if (!fs.existsSync(target)) {
      return;
    }

    var buildGradle = readBuildGradle(target);

    buildGradle = buildGradle.replace(/(?:^|\r?\n)(.*)pushwoosh-plugin*?(?=$|\r?\n)/g, '');
  
    writeBuildGradle(buildGradle, target);
}

function modifyBuildGradle() {
    var target = path.join("android", "build.gradle");
   
    if (!fs.existsSync(target)) {
      console.log("not exist:" + target);
      return;
    }
    var buildGradle = readBuildGradle(target);
    buildGradle = addDependencies(buildGradle);
    buildGradle = addRepos(buildGradle);
    writeBuildGradle(buildGradle, target);

    var target = path.join("android", "app", "build.gradle");

    if (!fs.existsSync(target)) {
      console.log("not exist:" + target);
      return;
    }
    var buildGradle = readBuildGradle(target);
    buildGradle = addPlugin(buildGradle);
    writeBuildGradle(buildGradle, target);
  }

function copyGooglePlayJson() {
    if (!fs.existsSync("google-services.json")) {
      console.log('google-services.json not exist, add google-services.json for correct work FCM');
      return;
    }
    fs.copyFile("google-services.json", path.join("android", "app","google-services.json"), (err) => {
      if (err) throw err;
      console.log('google-services.json was copied to android/app/google-services.json success');
    });
  }

  function restoreBuildGradle() {
    var target = path.join("android", "build.gradle");
    restore(target);
    target = path.join("android", "app",  "build.gradle");
    restore(target);
  }

restoreBuildGradle();
modifyBuildGradle();
copyGooglePlayJson();