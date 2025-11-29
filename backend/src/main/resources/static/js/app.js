document.addEventListener('htmx:configRequest', (event) => {
    const token = localStorage.getItem('token');
    if (token) {
        event.detail.headers['Authorization'] = `Bearer ${token}`;
    }
});

document.addEventListener('htmx:responseError', (event) => {
    if (event.detail.xhr.status === 401 || event.detail.xhr.status === 403) {
        localStorage.removeItem('token');
        window.location.href = '/login';
    }
});

window.auth = {
    login: async (username, password) => {
        try {
            const response = await fetch('/api/v1/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            if (response.ok) {
                const data = await response.json(); // Assuming token is returned directly or in object
                const token = data.token || data;
                localStorage.setItem('token', token);
                window.location.href = '/';
            } else {
                alert('Login failed');
            }
        } catch (e) {
            console.error(e);
            alert('Login error');
        }
    },
    logout: () => {
        localStorage.removeItem('token');
        window.location.href = '/';
    }
};
