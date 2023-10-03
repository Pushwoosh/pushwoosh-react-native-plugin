module.exports = {
  dependencies: {
    platforms: {
      ios: {
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
