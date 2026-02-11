/**
 * 
 */

document.addEventListener('DOMContentLoaded', function() {
	const forms = document.querySelectorAll('form');
	
	forms.forEach(form => {
		form.addEventListener('submit', function(e) {
			const submitBtn = this.querySelector('button[type="submit"]');
			
			if(!submitBtn || submitBtn.disabled) {
				return;
			}
			
			submitBtn.dataset.originalText = submitBtn.textContent;				
			submitBtn.disabled = true;
			submitBtn.textContent = '処理中';
		})
	})
})