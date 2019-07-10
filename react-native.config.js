module.exports = {
  dependency: {
    platforms: {
      ios: {
        sharedLibraries: ["libstdc++", "libz"],
        project: "./src/ios/PushwooshPlugin.xcodeproj"
      },
      android: {
        sourceDir: "./src/android"
      }
    },
    assets: [
    	"./src/ios/PushwooshInboxBundle.bundle"
    ]
  }
};