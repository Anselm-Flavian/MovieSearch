import React, { useEffect, useState } from 'react';
import { createAuth0Client } from '@auth0/auth0-spa-js';
import { BrowserRouter as Router, Route, Routes, useNavigate } from 'react-router-dom';
import Search from './Search';
import MovieGallery from './MovieGallery';
import MovieDetails from './MovieDetails';
import './App.css';

function AppContent() {
  const [auth0Client, setAuth0Client] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const initAuth0 = async () => {
      if (auth0Client) {
        setIsLoading(false);
        return;
      }

      try {
        const auth0 = await createAuth0Client({
          domain: 'dev-opqvt1nsdwq040ox.us.auth0.com',
          clientId: 'XkQrjGwmkJYjgBXhXf49ZL2FIHMc7vkk',
          authorizationParams: {
            redirect_uri: window.location.origin,
            audience: 'https://dev-opqvt1nsdwq040ox.us.auth0.com/api/v2/',
            scope: 'openid profile email read:movies',
          },
          cacheLocation: 'localstorage',
        });

        setAuth0Client(auth0);

        const urlParams = new URLSearchParams(window.location.search);
        const errorParam = urlParams.get('error');
        const errorDescription = urlParams.get('error_description');
        const code = urlParams.get('code');
        const state = urlParams.get('state');

        if (errorParam) {
          setError(errorDescription);
          setIsLoading(false);
          return;
        }

        if (code && state) {
          console.log('Handling redirect callback...', { code, state });
          try {
            // [FIX] Validate state against stored value
            const storedState = sessionStorage.getItem('auth0_state');
            if (!storedState || state !== storedState) {
              throw new Error('Invalid state parameter');
            }
            await auth0.handleRedirectCallback();
            sessionStorage.removeItem('auth0_state'); // Clean up
            console.log('Redirect callback successful');
            window.history.replaceState({}, document.title, window.location.pathname);
          } catch (err) {
            console.error('Callback error:', err.message);
            // [FIX] Show error instead of bypassing with getTokenSilently
            setError('Authentication failed: ' + err.message + '. Please try logging in again.');
            setIsLoading(false);
            return;
          }
        }

        const isAuth = await auth0.isAuthenticated();
        setIsAuthenticated(isAuth);

        if (isAuth && !user) {
          const userData = await auth0.getUser();
          setUser(userData);
          console.log('User:', userData);
        }

        setIsLoading(false);
      } catch (err) {
        console.error('Error initializing Auth0:', err);
        setError(err.message || 'Failed to initialize Auth0');
        setIsLoading(false);
      }
    };

    initAuth0();
  }, [auth0Client, navigate]);

  const login = async () => {
    if (auth0Client && !isLoggingIn) {
      setIsLoggingIn(true);
      try {
        // [FIX] Generate and store state for validation
        const state = Math.random().toString(36).substring(2);
        sessionStorage.setItem('auth0_state', state);
        await auth0Client.loginWithRedirect({ state });
      } catch (err) {
        console.error('Login error:', err);
        setError(err.message || 'Failed to initiate login');
      } finally {
        setIsLoggingIn(false);
      }
    }
  };

  const logout = async () => {
    if (auth0Client) {
      await auth0Client.logout({
        logoutParams: {
          returnTo: window.location.origin,
        },
      });
    }
  };

  const callApi = async () => {
    if (auth0Client) {
      try {
        // [NOTE] Remove Authorization header since endpoint is public
        const response = await fetch('http://localhost:8080/api/movies/search?query=action', {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
          mode: 'cors',
        });

        if (!response.ok) {
          const errorText = await response.text();
          console.error('API error:', errorText);
          // [FIX] Handle 503 specifically
          if (response.status === 503) {
            setError('Movie search is temporarily unavailable. Please try again later.');
            return;
          }
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        const data = await response.json();
        navigate('/results', { state: { initialData: data } });
      } catch (error) {
        console.error('Error calling API:', error);
        setError(error.message || 'Failed to call API');
      }
    }
  };

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return (
      <div>
        <h1>Error</h1>
        <p>{error}</p>
        <button onClick={() => setError(null)}>Clear Error</button>
        <button onClick={login}>Try Logging In Again</button>
      </div>
    );
  }

  return (
    <div className="App">
      <h1>Movie Search App</h1>
      {isAuthenticated ? (
        <div>
          <p>Welcome, {user?.name}!</p>
          <button onClick={logout}>Log Out</button>
          <button onClick={callApi}>Search Action Movies</button>
        </div>
      ) : (
        <button onClick={login}>Log In</button>
      )}
      <Routes>
        <Route
          path="/results"
          element={<ResultsPage auth0Client={auth0Client} isAuthenticated={isAuthenticated} />}
        />
        <Route
          path="/movie-details/:query"
          element={<MovieDetails auth0Client={auth0Client} isAuthenticated={isAuthenticated} />}
        />
        <Route
          path="/movies/:movieId"
          element={<MovieDetails auth0Client={auth0Client} isAuthenticated={isAuthenticated} />}
        />
        <Route path="*" element={null} />
      </Routes>
    </div>
  );
}

function ResultsPage({ auth0Client, isAuthenticated }) {
  return (
    <div>
      <Search auth0Client={auth0Client} isAuthenticated={isAuthenticated} />
      <MovieGallery auth0Client={auth0Client} isAuthenticated={isAuthenticated} />
    </div>
  );
}

function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}

export default App;