//
//  PWInlineInAppManager.m
//  pushwoosh-react-native-plugin
//
//  Created by Fectum on 15/01/2020.
//

#import "PWInlineInAppManager.h"

@interface PWReactInlineInAppView: PWInlineInAppView

@property (nonatomic) RCTBubblingEventBlock onLoaded;
@property (nonatomic) RCTBubblingEventBlock onClosed;
@property (nonatomic) RCTBubblingEventBlock onSizeChanged;

@end

@implementation PWReactInlineInAppView
@end

@implementation PWInlineInAppManager 

RCT_EXPORT_MODULE(PWInlineInAppView)

- (UIView *)view {
    PWReactInlineInAppView *view = [PWReactInlineInAppView new];
    view.delegate = self;
    return view;
}

RCT_EXPORT_VIEW_PROPERTY(identifier, NSString)
RCT_EXPORT_VIEW_PROPERTY(onLoaded, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onClosed, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onSizeChanged, RCTBubblingEventBlock)

- (void)inlineInAppDidLoadInView:(PWReactInlineInAppView *)inAppView {
    if (inAppView.onLoaded) {
        inAppView.onLoaded(@{@"identifier": inAppView.identifier});
    }
}

- (void)didCloseInlineInAppView:(PWReactInlineInAppView *)inAppView {
    if (inAppView.onClosed) {
        inAppView.onClosed(@{@"identifier": inAppView.identifier});
    }
}

- (void)didChangeSizeOfInlineInAppView:(PWReactInlineInAppView *)inAppView {
    if (inAppView.onSizeChanged) {
        inAppView.onSizeChanged(@{@"width": @(inAppView.bounds.size.width), @"height": @(inAppView.bounds.size.height)});
    }
}

@end
