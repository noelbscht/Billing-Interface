package net.libraya.gobdarchive.archive;

public enum EntryType {
    INVOICE,            // Rechnung (Eingangs- und Ausgangsrechnung)
    STORNO,             // Stornobeleg
    CREDIT_NOTE,        // Gutschrift
    REFUND,             // Rueckerstattung
    PAYMENT_RECEIPT,    // Zahlungsbeleg (z.B. Kontoauszug, Quittung)
    CONTRACT,           // Vertrag (Kauf-, Miet-, Dienstleistungsvertr�ge)
    TAX_DOCUMENT,       // Steuerdokumente (Steuerbescheide, Steuererkl�rungen)
    BUSINESS_CORRESPONDENCE, // Geschaeftskorrespondenz (inkl. steuerrelevanter E-Mails)
    INVENTORY_RECORD,   // Inventar- und Anlagenverzeichnisse
    PAYROLL_DOCUMENT,   // Lohn- und Gehaltsunterlagen
    FINANCIAL_STATEMENT,// Jahresabschluss, Bilanz, GuV
    TRAVEL_EXPENSE,     // Reisekostenabrechnung
    MILEAGE_LOG,        // Fahrtenbuch
    MEETING_MINUTES,    // Protokolle (z.B. Sitzungsprotokolle)
    OTHER               // Sonstige Dokumente
}
