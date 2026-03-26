/**
    hide all views on page load
*/
window.addEventListener('load', function() {
    // hide all views except the first one
    loadView(0);
});

/**
    display view by view id
*/
function loadView(viewId) {
    // list of all views
    const views = document.querySelectorAll("[data-viewid]");


    // current viewId
    viewId = Number(viewId);
    for (let i = 0; i < views.length; i++) {
        let currentView = views[i];
        if (i == viewId) {
            if (currentView.classList.contains('hidden')) {
                currentView.classList.remove('hidden');
            }
            continue;
        }
        if (!currentView.classList.contains('hidden')) {
            currentView.classList.add('hidden');
        }
    }
}