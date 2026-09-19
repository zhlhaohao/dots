(function() {
	if (window.jsbridge) {
		return;
	}

	var messageHandlers = {};
	var uniqueId = 1;

	//同步调用Native
	function syncSendToNative(methodName, params) {
		var returnValue = Android.syndMessageSend(methodName,JSON.stringify(params));
		return returnValue;
	}

	//异步调用Native
	function sendToNative(methodName, params, callBackClosureDict) {
		var callbackClosureId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
		if (callBackClosureDict) {
			messageHandlers[callbackClosureId] = callBackClosureDict;
		};
		Android.messageSend(methodName,callbackClosureId,JSON.stringify(params));
	}

	function callBackFromNative(callbackClosureId, type, paramsString) {

		if (callbackClosureId) {
			var callBackClosureDict = messageHandlers[callbackClosureId];
			var closure = callBackClosureDict[type];
			delete messageHandlers[callbackClosureId];
			if (closure) {
				// 原生 fail 兜底回传空 params（两参调用）时 paramsString 为 undefined，
				// 直接 JSON.parse 会抛 SyntaxError 导致回调闭包丢失 —— 此处兜底为空对象
				var dict = paramsString ? JSON.parse(paramsString) : {};
				closure(dict);
			}
			else{
				Android.hybrid(callbackClosureId);
			};
		};
	}

	//同步：获取屏幕信息（返回 string，调用方 JSON.parse）
	function getScreenInfo() {
		return syncSendToNative("getScreenInfo", {});
	}

	//异步：获取用户信息
	function getUserInfo(params) {
		params = params || {};
		sendToNative("getUserInfo", {}, {
			"success": params["success"],
			"fail":    params["fail"],
			"cancel":  params["cancel"]
		});
	}

	//同步：关闭当前页面
	function closeHtmlPage() {
		return syncSendToNative("closeHtmlPage", {});
	}

	//异步：告知原生返回键是否允许关闭页面（false 时返回键转调页面注册的 success 回调）
	function canGoBack(allow, onBack) {
		sendToNative("canGoBack", {
			"value": allow ? "true" : "false"
		}, {
			"success": onBack || function (res) { /* 返回键被拦截时触发, res.result == "true" */ }
		});
	}

	//异步：拉起系统相机拍照（仅拍照），压缩后 base64 回传（契约见 news 仓 docs/camera_bridge.md）
	function takePhoto(params) {
		params = params || {};
		sendToNative("takePhoto", {
			"quality":   params.quality   || 70,
			"maxWidth":  params.maxWidth  || 1280,
			"maxHeight": params.maxHeight || 1280
		}, {
			"success": params["success"],
			"fail":    params["fail"],
			"cancel":  params["cancel"]
		});
	}

	//异步：拉起系统相册选 1..maxCount 张照片，压缩后 base64 数组一次性回传（契约见 news 仓 docs/pickphotos_bridge.md）
	function pickPhotos(params) {
		params = params || {};
		sendToNative("pickPhotos", {
			"quality":   params.quality   || 70,
			"maxWidth":  params.maxWidth  || 1080,
			"maxHeight": params.maxHeight || 1080,
			"maxCount":  params.maxCount  || 1
		}, {
			"success": params["success"],
			"fail":    params["fail"],
			"cancel":  params["cancel"]
		});
	}


	//pickAndUploadFiles: multi-pick files, OkHttp multipart upload, aggregate single success callback
	function pickAndUploadFiles(params) {
		params = params || {};
		sendToNative("pickAndUploadFiles", {
			"uploadUrl":  params.uploadUrl  || "",
			"destDir":    params.destDir    || "",
			"overwrite":  params.overwrite  || false,
			"maxCount":   params.maxCount   || 9,
			"cookieName": params.cookieName || "cortex_auth"
		}, {
			"success": params["success"],
			"fail":    params["fail"],
			"cancel":  params["cancel"]
		});
	}

	//downloadFile: OkHttp stream download to system Downloads dir + notification
	function downloadFile(params) {
		sendToNative("downloadFile", {
			downloadUrl : params.downloadUrl || "",
			fileName    : params.fileName || "",
			cookieName  : params.cookieName || "cortex_auth"
		}, params.success, params.fail);
	}

	window.jsbridge = {
		syncSendToNative		: syncSendToNative,
		sendToNative 			: sendToNative,
		callBackFromNative		: callBackFromNative,
		getScreenInfo			: getScreenInfo,
		getUserInfo				: getUserInfo,
		closeHtmlPage			: closeHtmlPage,
		canGoBack				: canGoBack,
		takePhoto				: takePhoto,
		pickPhotos				: pickPhotos,
		pickAndUploadFiles		: pickAndUploadFiles,
		downloadFile			: downloadFile
	};

})();
