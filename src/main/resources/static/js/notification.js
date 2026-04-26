

class NotificationManager {
	constructor() {
		this.stompClient = null;
		this.isConnected = false;
		this.userId = this.getCurrentUserId();
	}
	
	connect() {
		// SockJsでWebSocket接続
		const socket = new SockJS('/ws');
		this.stompClient = Stomp.over(socket);
		
		// 接続
		this.stompClient.connect({},
			(frame) => this.onConnected(frame),
			(error) => this.onError(error)
		);
		
	}
	
	onConnected(frame) {
		console.log('WebSocket接続成功:', frame);
		this.isConnected = true;
		this.updateConnectionStatus(true);
		
		// 個人通知を購読
		this.stompClient.subscribe(`/queue/${this.userId}/notifications`,
			(message) => this.onPersonalNotification(message)
		);
		
		// 全体通知を購読
		this.stompClient.subscribe('/topic/notifications',
			(message) => this.onBroadcastNotification(message)
		);
		
		console.log(`通知購読開始 - User ID: ${this.userId}`);
	}
	
	
	onPersonalNotification(message) {
		const notification = JSON.parse(message.body);
		console.log('個人通知受信:', notification);
		this.displayNotification(notification, 'personal');
	}

	onBroadcastNotification(message) {
		const notification = JSON.parse(message.body);
		console.log('全体通知受信:', notification);
		this.displayNotification(notification, 'broadcast');
	}
	
	/**
	 * 通知を画面に表示
	 */
	displayNotification(notification, type) {
		
		const container = document.getElementById('notification-container');
		
		const notificationElement = document.createElement('div');
		notificationElement.className = `notification notification-${notification.type.toLowerCase()} ${type}`;

		// アイコンを決定
		let icon = '📢';
		if(notification.type === 'EXPENSE_APPROVED') {
			icon = '✅';
		} else if(notification.type === 'EXPENSE_REJECTED') {
			icon = '✖';
		} else if(notification.type === 'EXPENSE_SUBMITTED') {
			icon = '📝';
		}
		
		notificationElement.innerHTML = `
		<div class="notification-header">
			<span class="notification-icon">${icon}</span>
			<span class="notification-title">${notification.message}</span>
			<button class="notification-close"
			onclick="this.parentElement.parentElement.remove()">×</button>
		</div>
		<div class="notification-body">
			<p><strong>経費:</strong> ${notification.title}</p>
			<p><strong>金額:</strong> ${notification.amount}円</p>
			${notification.applicantName ? `<p><strong>申請者:</strong> ${notification.applicantName}</p>` : ''}
			${notification.approverName ? `<p><strong>承認者:</strong> ${notification.approverName}</p>` : ''}
		    <p class="notification-time">${new Date(notification.timestamp).toLocaleString()}</p>
		</div>
	    `;
		
		container.insertBefore(notificationElement, container.firstChild);
		
		if(Notification.permission === 'granted') {
			new Notification(notification.message, {
				body: `${notification.title} - ${notification.amount}円`,
				icon: '/images/notification-icon.png'
			});
		}
		
		setTimeout(() => {
			notificationElement.style.opacity = '0';
			setTimeout(() => notificationElement.remove(), 300);
		}, 5000);
		
	}
	/**
	 * エラー時の処理
	 */
	onError(error) {
		console.error('WebSocketエラー:', error);
		this.isConnected = false;
		this.updateConnectionStatus(false);
		
		setTimeout(() => {
			console.log('WebSocket再接続を試みます...');
			this.connect();
		}, 5000);
	}
	
	
	/**
	 * 接続状態表示を更新
	 */
	updateConnectionStatus(connected) {
		const statusElement = document.getElementById('connection-status');
		if(statusElement) {
			statusElement.textContent = connected ? '接続中🔵' : '切断 🔴';
			statusElement.className = connected ? 'connected' : 'disconnected';
		}
	}
	
	/**
	 * 現在ログインユーザーIDを取得
	 */
	getCurrentUserId() {
		const userIdElement = document.getElementById('current-user-id');
		if(userIdElement) {
			return userIdElement.dataset.userId;
		}
		
		return 1;
	}
	
	/**
	 * 切断
	 */
	disconnect() {
		if(this.stompClient !== null) {
			this.stompClient.disconnect();
			console.log('WebSocket切断');
		}
		this.isConnected = false;
		this.updateConnectionStatus(false);
		
	}
	
	/**
	 * ブラウザ通知の許可をリクエスト
	 */
	requestNotificationPermission() {
		if('Notification' in window && Notification.permission === 'default') {
			Notification.requestPermission().then(permission => {
				console.log('通知許可:', permission);
			});
		}
	}
}

let notificationManager;

// ページ読み込み時に自動接続
document.addEventListener('DOMContentLoaded', () => {
	notificationManager = new NotificationManager();
	notificationManager.connect();
	notificationManager.requestNotificationPermission();
});

//ページ離脱時に切断
window.addEventListener('beforeunload', () => {
	if(notificationManager) {
		notificationManager.disconnect();
	}
});
