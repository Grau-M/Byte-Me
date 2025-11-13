// Theme initialization
(function() {
    const userThemeInput = document.getElementById('darkThemeEnabled');
    if (userThemeInput) {
        // User is logged in, use their preference
        if (userThemeInput.value === 'true') {
            document.documentElement.classList.add('dark-mode');
            document.body.classList.add('dark-mode');
        }
    } else {
        // Guest user - always use light theme
        document.documentElement.classList.remove('dark-mode');
        document.body.classList.remove('dark-mode');
    }
})();

// Sticky Footer Logic: Fixed when no scroll, static when scrollable
(function() {
    const footer = document.getElementById('main-footer');
    if (!footer) return;

    function checkFooterPosition() {
        const hasScroll = document.documentElement.scrollHeight > window.innerHeight;
        if (hasScroll) {
            footer.classList.add('has-scroll');
        } else {
            footer.classList.remove('has-scroll');
        }
    }

    // Check on page load
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', checkFooterPosition);
    } else {
        checkFooterPosition();
    }

    // Recheck on window resize
    window.addEventListener('resize', checkFooterPosition);
})();

// Dynamic Clock Script (common to dashboard and settings)
function updateClock() {
    const clockElement = document.getElementById('local-clock');
    if (!clockElement) return;

    const now = new Date();
    const userTimezone = localStorage.getItem('userTimezone') || 'America/Toronto';
    const formatter = new Intl.DateTimeFormat('en-US', {
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
        hour12: true,
        timeZone: userTimezone
    });

    const formattedTime = formatter.format(now);
    
    if (clockElement) {
        clockElement.textContent = formattedTime;
    }
}

// Only run clock update if element exists (for dashboard/settings)
if (document.getElementById('local-clock')) {
    updateClock();
    setInterval(updateClock, 1000);
}

// Theme switching logic (specific to settings page)
const darkModeSwitch = document.getElementById('darkModeSwitch');
if (darkModeSwitch) {
    const userThemeInput = document.getElementById('darkThemeEnabled');
    darkModeSwitch.checked = userThemeInput ? userThemeInput.value === 'true' : false;

    darkModeSwitch.addEventListener('change', () => {
        if (darkModeSwitch.checked) {
            document.documentElement.classList.add('dark-mode');
            document.body.classList.add('dark-mode');
        } else {
            document.documentElement.classList.remove('dark-mode');
            document.body.classList.remove('dark-mode');
        }
        if (userThemeInput) {
            userThemeInput.value = darkModeSwitch.checked;
        }
    });
}

// Handle form submission for theme (specific to settings page)
const themeForm = document.getElementById('theme-form');
if (themeForm) {
    themeForm.addEventListener('submit', (event) => {
        event.preventDefault();
        alert('Theme preference saved!');
    });
}
