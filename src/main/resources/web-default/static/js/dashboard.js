class ArchiveAction {
	
	static async commit(fileInput, metadata = {}) {
		return this.#submit({file: fileInput, metadata: metadata, action: "commit"}, true, false);
	}
	
	static async view(archiveId) {
		return this.#submit({archive_id: archiveId, action: "view"});
	}

	static async verify(archiveId) {
		return this.#submit({archive_id: archiveId, action: "verify"});
	}
	
	static async exportSingle(archiveId, options = {}) {
		return this.#submit({archive_id: archiveId, export_options: JSON.stringify(options), action: "exportSingle"}, false, true);
	} 
	
	static async exportFiltered(options, filter = {}) {
		return this.#submit({export_options: JSON.stringify(options), filter: JSON.stringify(filter), action: "export"}, false, true);
	}
	
	static async query(filter) {
		return this.#submit({filter: JSON.stringify(filter), action: "query"}, false, false);
	}
	
	static async #submit(args = {}, upload = false, fileExpected = false) {

	    let body;
	    const headers = {};

	    if (upload) {
	        // file upload
	        body = new FormData();

	        for (const [key, value] of Object.entries(args)) {

	            if (key === "file") {
	                body.append("file", value.files[0]); // file
	            } else if (typeof value === "object") {
	                body.append(key, JSON.stringify(value));
	            } else {
	                body.append(key, value);
	            }
	        }
	    } else {
	        // normal request with content-type
	        body = new URLSearchParams();
	        for (const [key, value] of Object.entries(args)) {
	            body.append(key, value);
	        }
	        headers["Content-Type"] = "application/x-www-form-urlencoded";
	    }

	    const response = await fetch(window.location.href, {
	        method: "POST",
	        headers,
	        body
	    });

	    // download
	    if (response.ok && fileExpected) {
			const filename = (args.archive_id != null ? args.archive_id : (args.export_options != null ? "export_" + args.filter : "export")) + ".zip";
			
	        const blob = await response.blob();
	        const url = URL.createObjectURL(blob);

	        const a = document.createElement("a");
	        a.href = url;
	        a.download = filename;
	        a.click();

	        URL.revokeObjectURL(url);
	    }
		
		//todo:: remove later
		try {
		    const data = await response.clone().json();

		    if (data.message) {
		        sendMessage("server: " + data.message, "info", 5);
		    }
		} catch (ignored) {}


	    return response;
	}
}

function verify(archiveId) {
	ArchiveAction.verify(archiveId)
		.then(resp => {
	        if (resp.ok) {
	            sendMessage("Die Datei ist im Originalzustand.", "success", 2.5);
	        } else {
	            sendMessage("Die Datei ist nicht im Originalzustand.", "error", 2.5);
				console.log(resp.json().errors);
	        }
	    }).catch(err => {
	        console.error(err);
	        sendMessage("Fehler beim Upload.", "error", 2.5);
	    });
}

function exportSingle(archiveId) {
	const popup = new PopupExportOptions(archiveId);
	
	popup.initialize();
	popup.show();
}

function commit() {
	// collect all inputs from upload window
	const inputs = document.querySelector("#upload").querySelectorAll('input, select, textarea');

	const metadata = {};
	let fileInput;
    for (const el of inputs) {
		
		// fill metadata
		if (el.tagName == "SELECT") {
			const selectedIndex = document.querySelector("#upload > select").selectedIndex;
			if (selectedIndex != 0) {
				metadata[el.name] = el.children[selectedIndex].value;
			}
		}
		
		if (el.type == "text") {
			metadata[el.name] = el.value;
			continue;
		}
		
		if (el.type == "file") {
			fileInput = el;
		}
	}
	
	// no file selected
	if (fileInput.files.length == 0) {
		sendMessage("Keine Datei ausgewählt.", "info", 1.5);
		return;
	}
	
	ArchiveAction.commit(fileInput, metadata)
	    .then(resp => {
	        if (resp.ok) {
	            sendMessage("Upload erfolgreich.", "success", 2.5);
				window.location.reload();
	        } else {
	            sendMessage("Upload fehlgeschlagen.", "error", 2.5);
	        }
	    }).catch(err => {
	        console.error(err);
	        sendMessage("Fehler beim Upload.", "error", 2.5);
	    });

}

async function view(archiveId) {
	// request document
	const request = await ArchiveAction.view(archiveId);
	
	if (!request.ok) {
		sendMessage("...", "error", 2.5);
		return;
	} 
	
	const json = await request.json();
	
	const popup = new PopupDocument(archiveId, json);
	
	popup.initialize();
	popup.show();	
}
