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
		return (
			<PWInlineInAppView 
				{...this.props} 
				onLoaded = {this._onLoaded}
				onClosed = {this._onClosed}
				onSizeChanged = {this._onSizeChanged}
			/>
		);
	}
}

InlineInAppView.propTypes = {
	/**
	 * Value of the identifier property must be equal to the 
	 * identifier attribute value of the in-app message you've 
	 * created in Pushwoosh Control Panel
	 */
	identifier: PropTypes.string,
	/**
     * This event is called to notify you that an inline in-app
     * was loaded and has been added to the view
     */
	onLoaded: PropTypes.func,
	 /**
     * This event is called to notify you that an inline in-app
     * view has been closed by the user
     */
	onClosed: PropTypes.func,
	/**
     * This event is called to notify you that an inline in-app
     * view size has been changed
     */
	onSizeChanged: PropTypes.func,
};

var PWInlineInAppView = requireNativeComponent('PWInlineInAppView', InlineInAppView)

export default InlineInAppView