let messageElements = [];

/**
	displays a div with the given 'message' text
	for 'seconds' seconds.
*/
function sendMessage(message, category, seconds) {
	let msgElement = document.createElement('div');

	msgElement.classList.add('flash-text');
	switch (category.toLowerCase()) {
	    case 'success':
	        msgElement.classList.add('flash-succ');
	        break;
	    case 'error':
	        msgElement.classList.add('flash-err');
	        break;
	    default:
	        break
	}
	msgElement.innerHTML = message;
	setTimeout(function () {
	    messageElements.pop(msgElement);
		document.children[0].removeChild(msgElement);
		updateMessages();
	}, seconds * 1000);

    messageElements.push(msgElement);
	document.children[0].appendChild(msgElement);
	updateMessages();
}

function updateMessages() {
        let messageElements = document.querySelectorAll('.flash-text');
		
        Array.from(messageElements).reverse().forEach((msgElement, index) => {
            msgElement.style.top = (15 + (index * 50)) + "px";
    });
}