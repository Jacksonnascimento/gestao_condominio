document.addEventListener('DOMContentLoaded', function() {
    const sidebar = document.getElementById('sidebar');
    const toggleButton = document.getElementById('sidebar-toggle-btn');

    if (sidebar && toggleButton) {
        toggleButton.addEventListener('click', function() {
            sidebar.classList.toggle('show');
        });

        document.addEventListener('click', function(event) {
            const isClickInsideSidebar = sidebar.contains(event.target);
            const isClickOnToggleButton = toggleButton.contains(event.target);

            if (!isClickInsideSidebar && !isClickOnToggleButton && sidebar.classList.contains('show')) {
                sidebar.classList.remove('show');
            }
        });
    }
});