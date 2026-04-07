class Popup {
    constructor(title, options = {}) {
        this.title = title;
        this.dark = options.dark || false;
        this.centered = options.centered || false;
        this.closeable = options.closeable || true;
        this.animation = options.animation || true;
        this.container = document.createElement('div');
        this.content = document.createElement('div');
		this.inputValues = options.inputValues || {};
		this.attachedTo = options.attachedTo;
		
		
        // bind methods
        this.hide = this.hide.bind(this);
        this.show = this.show.bind(this);
    }

    initialize() {
        // clear old content
        this.content.innerHTML = '';
		
        // set classes
    	this.container.classList.add(this.attachedTo == null ? 'popup' : 'attached-popup');
        this.content.classList.add('popup-content');
		
        if (this.centered) {
            this.content.classList.add('center');
        }
        if (!this.animation) {
            this.container.style.animation = 'none';
            this.content.style.animation = 'none';
        }
        if (this.dark) this.content.classList.add('bg-dark');

        // load content
        const closeNode = DOMPreset.button('', 'close', this.hide);
        const titleNode = document.createElement('h3');
        titleNode.innerHTML = this.title;

        if (this.closeable) this.content.appendChild(closeNode);
        this.content.appendChild(titleNode);
        this.content.appendChild(document.createElement('hr'));

        for (const el of this.getContent()) {
            this.content.appendChild(el);
        }

        // append elements
        document.body.appendChild(this.container);
        this.container.appendChild(this.content);
		
		this.#updateInputValues(); // add value auto updating
    }

    async reload() {
        // clear old content
        this.content.innerHTML = '';

        // load content
        const closeNode = DOMPreset.button('', 'close', this.hide);
        const titleNode = document.createElement('h3');
        titleNode.innerHTML = this.title;

        if (this.closeable) this.content.appendChild(closeNode);
        this.content.appendChild(titleNode);
        this.content.appendChild(document.createElement('hr'));

        for (const el of this.getContent()) {
            this.content.appendChild(el);
        }
		
		this.#updateInputValues(); // add value auto updating
    }
	
	/**
	 * auto save values from all inputs
	 */
	#updateInputValues() {
		const inputs = this.content.querySelectorAll('input, select, textarea');

	    for (const el of inputs) {
	        const name = el.name;
	        if (!name) continue;
			
			// single update on call
			this.inputValues[name] = this.#getElementValue(el);
			
			// add change event
	        el.addEventListener('change', () => {
	            this.inputValues[name] = this.#getElementValue(el);
	        });
	    }
	}
	
	#getElementValue(element) {
		if (element.type === 'checkbox' || element.type === 'radio') {
	        return element.checked;
	    }
	    return element.value;
	}

    isVisible() {
        return this.container.classList.contains('show');
    }

    show() {
        this.container.classList.add('show');
		
		// position popup correctly
		const isAttached = this.attachedTo != null;
		if (isAttached) {
			const bounding = this.attachedTo.getBoundingClientRect();
			this.container.style.top = bounding.y + (bounding.height * 0.50) + "px";
			this.container.style.left = bounding.x + (bounding.width * 0.50) + "px";
		}
    }

    hide() {
        this.container.classList.remove('show');
    }

    toggle() {
        this.isVisible() ? this.hide() : this.show();
    }

    getContent() {
        throw new Error("method must be implemented.");
    }
}

class PopupSelectReference extends Popup {
	constructor(entries = [], resultInput, attachedTo) {
		super("Referenz auswählen", {attachedTo: attachedTo})
		this.entries = entries;
		this.resultInput = resultInput;
	}
	
	getContent() {
		
	}
}

class PopupExportOptions extends Popup {
	constructor(archiveId=null) {
		super("Exportoptionen", {centered: false});
		this.archiveId = archiveId || null;
	}
	
	getContent() {
		const elements = [];
		
		// include auditlog option
        const title = Translation.getTranslation("german", "include_audit_log");
        const sw = DOMPreset.switchOption("include_audit_log", title, false);

        elements.push(sw.wrapper);
		
		// submit button
        const submitBtn = DOMPreset.button("Exportieren", "success", () => this.#submit());
        elements.push(submitBtn);
		
		return elements;
	}
	
	async #submit() {
		// hide popup
		this.hide();
		
		// single export if declared with archiveId parameter
		if (this.archiveId != null) {
			const resp = await ArchiveAction.exportSingle(this.archiveId, this.inputValues);
			if (resp.status != 200) {
				sendMessage("Export fehlgeschlagen.", "error");
				return;
			}
			return;
		}
		
		const resp = await ArchiveAction.exportFiltered(this.inputValues);
		if (resp.status != 200) {
			sendMessage("Export fehlgeschlagen.", "error");
			return;
		}
	}
}

class PopupListFilter extends Popup {
	constructor(types = [], outputList, attachedTo) {
		super("Listenfilter", {attachedTo: attachedTo, centered: true});
		this.types = types || [];
		this.outputList = outputList;
	}
	
	getContent() {
        const elements = [];

        // type (EntryType Enum)
		const entryTypes = [];
		entryTypes.push();
		for (const type of this.types) {
			entryTypes.push({value: type, text: Translation.getTranslation("german", type)});
		}
		
        const typeSelect = DOMPreset.selectInput(
            entryTypes,
            (this.inputValues.type != null ? this.inputValues.type : "") || null, // auto selection
       		"Eintragstyp wählen"
		 );
		
        typeSelect.name = "entry_type";
        elements.push(typeSelect);

        // from date
        const fromInput = document.createElement("input");
        fromInput.type = "date";
        fromInput.name = "from";
        fromInput.value = this.inputValues.from || null;
        fromInput.classList.add("form-control", "sp-btm");
        elements.push(fromInput);

        // to date
        const toInput = document.createElement("input");
        toInput.type = "date";
        toInput.name = "to";
        toInput.value = this.inputValues.to || null;
        toInput.classList.add("form-control", "sp-btm");
        elements.push(toInput);

        // custom key
        const customKey = DOMPreset.textInput(
            "Metadaten-Key",
            this.inputValues.customKey || null
        );
        customKey.name = "custom_key";
        elements.push(customKey);

        // custom value
        const customValue = DOMPreset.textInput(
            "Metadaten-Wert",
            this.inputValues.customValue || null
        );
        customValue.name = "custom_value";
        elements.push(customValue);

        // hash
        const hash = DOMPreset.textInput(
            "Hashwert",
            this.inputValues.hash || null
        );
        hash.name = "hash";
        elements.push(hash);

        // archiveId
        const archiveId = DOMPreset.textInput(
            "Archiv ID",
            this.inputValues.archiveId || null
        );
        archiveId.name = "archive_id";
        elements.push(archiveId);

        // submit button
        const submitBtn = DOMPreset.button("Filter anwenden", "success", () => this.#submit(this.outputList));
        elements.push(submitBtn);
		
        return elements;
    }
	
	/**
	 * add list items to a table content row by copying a reference row and replace them.
	 */
	#updateListItems(tableBody, refRow, entries) {
		// clear old entries (ignore table headline)
		for (let i = tableBody.children.length - 1; i > 0; i--) {
			tableBody.removeChild(tableBody.children[i]);
		}
		
		// clone reference row with children
		const template = refRow.cloneNode(true);
		
		if (entries.length === 0) {
	        const row = document.contains(template) ? template : template.cloneNode(true);
	        row.children[1].innerHTML = "Keine Einträge gefunden.";
			this.#updateActionButtons(row.children[2], -1, true);
	        tableBody.appendChild(row);
	        return;
	    }

		
		for (const entry of entries) {
			const row = document.contains(template) ? template : template.cloneNode(true);
			
			const archiveId = entry["archive_id"];
			const archivePath = entry["file_path"];
			
			const nameCol = row.children[1];
			const actionCol = row.children[2]; 
			
			nameCol.innerHTML = archivePath; // fill name column
			
			this.#updateActionButtons(actionCol, archiveId);
			
			tableBody.appendChild(row);
		}
	}
	
	#updateActionButtons(actionColumn, archiveId, hide=false) {
		for (const child of actionColumn.children) {
			 child.disabled = false;
			 child.style.opacity = hide ? 0 : 1;
	 	}
		actionColumn.children[0].setAttribute("onclick", (hide ? ""  : "view('" + archiveId + "')")); // view btn
		actionColumn.children[1].setAttribute("onclick", (hide ? ""  : "verify('" + archiveId + "')")); // verify btn
		actionColumn.children[2].setAttribute("onclick", (hide ? ""  : "exportSingle('" + archiveId + "')")); // export btn	
	}
	
	async #submit(table) {
		// hide popup
		this.hide();

		const resp = await ArchiveAction.query(this.inputValues);
		const tableBody = table.children[0]; // table content row
		
		if (resp.status != 200) {
			sendMessage("Auflistung fehlgeschlagen.", "error");
			return;
		}
		const data = await resp.json();
		const entries = data.result || [];
		console.log(entries);
		
		this.#updateListItems(tableBody, tableBody.children[1], entries);
		sendMessage("Filter angewendet.", "success", 2.5);
	}
}

class PopupDocument extends Popup {
	constructor(archiveId, json) {
		super("Dokument: " + archiveId);
		this.archiveId = archiveId;
		
		this.fileUrl = "data:" + json.mime + ";base64," + json.file;
		this.filename = json.filename;
		this.metadataText = atob(json.metadata);
	}
	
	showOriginal() {
		this.viewOriginalElement.style.display = "block";
        this.viewMetadataElement.style.display = "none";

	}
	
	showMetadata() {
		this.viewOriginalElement.style.display = "none";
        this.viewMetadataElement.style.display = "block";

	}
	
	getContent() {
		this.content.style.minHeight = "95%";
		this.content.style.minWidth = "60%";
		
		this.viewOriginalElement = document.createElement("iframe");
        this.viewOriginalElement.src = this.fileUrl;
        this.viewOriginalElement.style.height = "100%";
		this.viewOriginalElement.style.width = "100%";
        this.viewOriginalElement.style.border = "none";

        this.viewMetadataElement = document.createElement("pre");
        this.viewMetadataElement.innerText = this.metadataText;
		this.viewMetadataElement.style.backgroundColor = "#eee";
        this.viewMetadataElement.style.display = "none";
		this.viewOriginalElement.style.height = "100%";
		this.viewOriginalElement.style.width = "100%";
        this.viewMetadataElement.style.overflow = "auto";
		
		
		// tablist
		const tabOriginalBtn = DOMPreset.iconButton(this.filename != null ? this.filename : "Originaldatei",
	  		["fa-regular", "fa-file", "fa-xs"], ["btn-primary", "btn-block"], () => this.showOriginal());
		const tabMetaBtn = DOMPreset.iconButton("Metadata.json",
			["fa-regular", "fa-file", "fa-xs"], ["btn-primary", "btn-block"], () => this.showMetadata());
		
		const tablist = DOMPreset.tablist("Tabs:", [tabOriginalBtn, tabMetaBtn]);
		
		const content = document.createElement("div");
		
		content.style.width = "85vw";
		content.style.height = "90vh";
		content.style.zoom = "80%";
		
	    content.appendChild(tablist);
	    content.appendChild(this.viewOriginalElement);
	    content.appendChild(this.viewMetadataElement);


        return [content];
    }

}

class DOMPreset {
	static switchOption(name, label, value = false) {
	    const wrapper = document.createElement("div");
	    wrapper.className = "form-check form-switch";

	    const input = document.createElement("input");
		input.name = name;
	    input.className = "form-check-input";
	    input.type = "checkbox";
	    input.checked = value;

	    const id = "switch-" + Math.random().toString(36).substring(2);
	    input.id = id;

	    const lbl = document.createElement("label");
	    lbl.className = "form-check-label";
	    lbl.htmlFor = id;
	    lbl.innerText = label;

	    wrapper.appendChild(input);
	    wrapper.appendChild(lbl);

	    return { wrapper, input };
	}

	
    static button(text, style, onclick = null) {
	    const node = document.createElement('button');
	    node.innerHTML = text;
	    node.type = 'button';
	    node.classList.add('btn', `btn-${style}`, 'sp-btm');
	
	    if (onclick) node.addEventListener('click', onclick);
	
	    return node;
	}

    static iconButton(text, iClassList, bClassList, onclick = null) {
        const node = document.createElement('button');
        const ico = document.createElement('i');

        iClassList.forEach(c => ico.classList.add(c));
        node.appendChild(ico);

        node.innerHTML += ` ${text}`;
        node.type = 'button'; 
        node.classList.add('btn', 'sp-btm');
        bClassList.forEach(c => node.classList.add(c));

        if (onclick) node.addEventListener('click', onclick);

        return node;
    }

    static imageIcon(url, width, height) {
        const img = document.createElement("img");

        img.src = url;
        img.width = width;
        img.height = height;

        return img;
    }

    static textButton(text, onclick = null, href = null) {
        const node = document.createElement('a');
        node.innerHTML = text;
        if (onclick) node.addEventListener('click', onclick);
        if (href) node.href = href;

        return node;
    }

    static selectionButton(text, iconClass, route, callback) {
        const button = DOMPreset.iconButton(text, ['fa', iconClass], ['btn-info']);
        button.addEventListener('click', () => {
            new PopupSelection(route, callback).show();
        });
        return button;
    }

    static selectInput(options = [], selectedValue = '', placeholder = "") {
        const node = document.createElement('select');
        node.classList.add('form-select', 'sp-btm');
		
		if (placeholder !== "") {
			const opt = document.createElement('option');
			opt.innerHTML = "-- " + placeholder + " --";
			opt.value = "";
			opt.disabled = true;
			opt.selected = true;
			node.appendChild(opt);
		}
		
        options.forEach(option => {
            const opt = document.createElement('option');
            opt.value = option.value;
            opt.innerHTML = option.text;
            if (option.value === selectedValue) {
                opt.selected = true;
            }
            node.appendChild(opt);
        });

        return node;
    }

    static textInput(placeholder = '', value = '', required = false, maxLength = 255) {
        const node = document.createElement('input');
        node.type = 'text';
        node.placeholder = placeholder;
        node.maxLength = maxLength;
        node.value = value;
        node.classList.add('form-control', 'sp-btm');

        if (required) {
            node.required = true;
        }

        return node;
    }
	
	static tablist(title, tabButtons = []) {
		const table = document.createElement("table");
		const tbody =  document.createElement("tbody");
		const titlerow = document.createElement("tr");
		const tabrow = document.createElement("tr");
		
		table.classList.add("file-table", "sp-btm");
		table.appendChild(tbody);
		
		// title row
		titlerow.classList.add("headline-row");
		const titleCell = document.createElement("td");
	    titleCell.innerText = title;
	    titlerow.appendChild(titleCell);
		for (let i = 1; i < tabButtons.length; i++) {
	        titlerow.appendChild(document.createElement("td"));
	    }
		tbody.append(titlerow);
		
		// tab button row
		tabrow.classList.add("tbl-itm-row");
	    for (let i = 0; i < tabButtons.length; i++) {
	        const btnColumn = document.createElement("td");
	        btnColumn.appendChild(tabButtons[i]);
	        tabrow.appendChild(btnColumn);
	    }
	    tbody.appendChild(tabrow);

	    return table;

	}

    static table(headers, rows) {
        const table = document.createElement('table');
        table.classList.add('table', 'sp-btm');

        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        headers.forEach(header => {
            const th = document.createElement('th');
            th.innerText = header;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        const tbody = document.createElement('tbody');
        rows.forEach(rowData => {
            const row = document.createElement('tr');
            rowData.forEach(cellData => {
                const td = document.createElement('td');

                if (Array.isArray(cellData)) {
                    cellData.forEach(el => td.appendChild(el));
                } else if (cellData instanceof HTMLElement) {
                    td.appendChild(cellData);
                } else {
                    td.innerText = cellData;
                }
                row.appendChild(td);
            });
            tbody.appendChild(row);
        });
        table.appendChild(tbody);

        return table;
    }
}