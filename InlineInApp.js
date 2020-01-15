import PropTypes from 'prop-types';
import React from 'react';
import {requireNativeComponent} from 'react-native';

class InlineInAppView extends React.Component {
	_onLoaded = (event) => {
		if (!this.props.onLoaded) {
			return;
		}

		// process raw event...
		this.props.onLoaded(event.nativeEvent);
	}
	_onClosed = (event) => {
		if (!this.props.onClosed) {
			return;
		}

		// process raw event...
		this.props.onClosed(event.nativeEvent);
	}
	_onSizeChanged = (event) => {
		if (!this.props.onSizeChanged) {
			return;
		}

		// process raw event...
		this.props.onSizeChanged(event.nativeEvent);
	}
	render() {
		return <InlineInAppView {...this.props} />;
	}
}

InlineInAppView.propTypes = {
	/**
	 * A Boolean value that determines whether the user may use pinch
	 * gestures to zoom in and out of the map.
	 */
	identifier: PropTypes.string,
	/**
	 * Callback that is called continuously when the user is dragging the map.
	 */
	onLoaded: PropTypes.func,
	/**
	 * Callback that is called continuously when the user is dragging the map.
	 */
	onClosed: PropTypes.func,
	/**
	 * Callback that is called continuously when the user is dragging the map.
	 */
	onSizeChanged: PropTypes.func,
};

module.exports = requireNativeComponent('PWInlineInAppView', InlineInAppView);