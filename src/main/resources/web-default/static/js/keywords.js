class Translation {
	
	static languages = [];
	
	static getTranslation(language, key) {
		for (const lang of this.languages) {
			if (lang.name.toLowerCase() === language.toLowerCase()) {
				return lang.getKeyword(key);
			}
		}
		
		// return from first langauge
		return this.languages[0].getKeyword(key);
	}
	
	static getKeyByValue(language, input) {
		for (const lang of this.languages) {
			if (lang.name.toLowerCase() === language.toLowerCase()) {
				return lang.getKeyByValue(input);
			}
		}
		
		// return from first langauge
		return this.languages[0].getKeyByValue(input);
	}
}

class Language {
	
	constructor(name, context = {}) {
		this.name = name;
		this.context = context;
	}
	
	getKeyword(key) {
		if (this.context[key.toLowerCase()] == null) {
			return key; // return key if not found 
		}
		
		return this.context[key.toLowerCase()];
	}
	
	getKeyByValue(value) {
		for (const [k, v] of Object.entries(this.context)) {
			if (v.toLowerCase() === value.toLowerCase()) {
				return k;
			}
		}
		
		// return null if not found
		return null;
	}
}

// add languages
window.addEventListener('load', function() {
	Translation.languages.push(new Language("german", {
		// entry types
		"invoice": "Rechnung",
		"storno": "Stornobeleg", 
		"credit_note": "Gutschrift",
		"refund": "Rückerstattung",
		"payment_receipt": "Zahlungsbeleg",
		"contract": "Vertrag", 
		"tax_document": "Steuerdokumente",
		"business_correspondence": "Geschäftskorrespondenz", 
		"inventory_record": "Inventar- und Anlagenverzeichnisse", 
		"payroll_document": "Lohn- und Gehaltsunterlagen",
		"financial_statement": "Jahresabschluss, Bilanz, GuV",
		"travel_expense": "Reisekostenabrechnung",
		"mileage_log": "Fahrtenbuch",
		"meeting_minutes": "Protokolle", 
		"other": "Sonstieges",  
		// export options
		"include_audit_log": "Auditlog beifügen",
		"nested": "Verschachtelt",
		"interpret_datev": "DATEV interpretieren"
	}));
});