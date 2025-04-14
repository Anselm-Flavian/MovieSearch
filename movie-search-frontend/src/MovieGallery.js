import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const MovieGallery = ({ auth0Client, isAuthenticated }) => {
  const initialState = {
    genre: '',
    director: '',
    year: ''
  };

  const [query, setQuery] = useState('');
  const [movies, setMovies] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [genre, setGenre] = useState(initialState.genre);
  const [director, setDirector] = useState(initialState.director);
  const [year, setYear] = useState(initialState.year);
  const [favorites, setFavorites] = useState(new Set());

  const navigate = useNavigate();
  const baseImageUrl = 'https://image.tmdb.org/t/p/w500';
  const backendUrl = 'http://localhost:8080/api/movies';

  const fetchMovies = async (searchQuery) => {
    if (!searchQuery || !isAuthenticated || !auth0Client) {
      setMovies([]);
      setError('Please enter a search query and ensure you are logged in.');
      return;
    }
    setLoading(true);
    setError(null);

    const params = new URLSearchParams({ query: searchQuery });
    if (genre) params.append('genre', genre);
    if (director) params.append('director', director);
    if (year) params.append('year', year);
    const url = `${backendUrl}/search?${params.toString()}`;

    try {
      const user = await auth0Client.getUser();
      console.log('User info:', user);
      const token = await auth0Client.getTokenSilently({
        authorizationParams: {
          audience: 'https://dev-opqvt1nsdwq040ox.us.auth0.com/api/v2/',
          scope: 'openid profile email'
        }
      });
      console.log('Fetching movies with token:', token.substring(0, 10) + '...');
      const response = await fetch(url, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch movies: ${response.status} ${response.statusText} - ${errorText}`);
      }
      const data = await response.json();
      console.log('Movies fetched:', data);
      setMovies(data || []);
    } catch (err) {
      console.error('Fetch movies error:', err);
      setError('Failed to load movies: ' + err.message);
      setMovies([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchSuggestions = async (prefix) => {
    if (!prefix || !isAuthenticated || !auth0Client) {
      setSuggestions([]);
      return;
    }
    const url = `${backendUrl}/autocomplete?prefix=${encodeURIComponent(prefix)}`;
    try {
      const user = await auth0Client.getUser();
      console.log('User info for suggestions:', user);
      const token = await auth0Client.getTokenSilently({
        authorizationParams: {
          audience: 'https://dev-opqvt1nsdwq040ox.us.auth0.com/api/v2/',
          scope: 'openid profile email'
        }
      });
      console.log('Fetching suggestions with token:', token.substring(0, 10) + '...');
      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch suggestions: ${response.status} ${response.statusText} - ${errorText}`);
      }
      const data = await response.json();
      console.log('Suggestions fetched:', data);
      setSuggestions(data || []);
    } catch (err) {
      console.error('Fetch suggestions error:', err);
      setSuggestions([]);
      setError('Failed to load suggestions: ' + err.message);
    }
  };

  const fetchHistory = async () => {
    if (!isAuthenticated || !auth0Client) {
      setHistory([]);
      setError('Please log in to view search history.');
      return;
    }
    try {
      const user = await auth0Client.getUser();
      console.log('User info for history:', user);
      const token = await auth0Client.getTokenSilently({
        authorizationParams: {
          audience: 'https://dev-opqvt1nsdwq040ox.us.auth0.com/api/v2/',
          scope: 'openid profile email'
        }
      });
      console.log('Fetching history with token:', token.substring(0, 10) + '...');
      const response = await fetch(`${backendUrl}/history`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch search history: ${response.status} ${response.statusText} - ${errorText}`);
      }
      const data = await response.json();
      const uniqueHistory = Array.from(
        new Map(data.map(item => [item.movieId, item])).values()
      );
      console.log('History fetched:', uniqueHistory);
      setHistory(uniqueHistory || []);
    } catch (err) {
      console.error('Error fetching history:', err);
      setError('Failed to load search history: ' + err.message);
      setHistory([]);
    }
  };

  const selectMovie = async (movie) => {
    if (!isAuthenticated || !auth0Client) return;
    try {
      const token = await auth0Client.getTokenSilently({
        authorizationParams: {
          audience: 'https://dev-opqvt1nsdwq040ox.us.auth0.com/api/v2/',
          scope: 'openid profile email'
        }
      });
      const response = await fetch(`${backendUrl}/select`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          id: movie.id,
          title: movie.title,
          poster_path: movie.poster_path
        })
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to save selected movie: ${response.status} ${response.statusText} - ${errorText}`);
      }
      console.log('Movie selected:', movie.title);
      await fetchHistory();
    } catch (err) {
      console.error('Error selecting movie:', err);
      setError(`Failed to select movie: ${err.message}`);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchMovies(query);
    setSuggestions([]);
  };

  const handleInputChange = (e) => {
    const value = e.target.value;
    setQuery(value);
    if (value.length > 1) fetchSuggestions(value);
    else setSuggestions([]);
  };

  const handleSuggestionClick = (suggestion) => {
    setQuery(suggestion);
    fetchMovies(suggestion);
    setSuggestions([]);
  };

  const toggleFavorite = (movieId, e) => {
    e.stopPropagation();
    const newFavorites = new Set(favorites);
    if (newFavorites.has(movieId)) {
      newFavorites.delete(movieId);
    } else {
      newFavorites.add(movieId);
    }
    setFavorites(newFavorites);
  };

  useEffect(() => {
    if (isAuthenticated && auth0Client) {
      fetchHistory();
    } else if (!isAuthenticated && auth0Client) {
      auth0Client.loginWithRedirect();
    }
  }, [isAuthenticated, auth0Client]);

  if (!isAuthenticated) return null;

  return (
    <div style={{ padding: '20px' }}>
      <h1>Movie Gallery</h1>

      <form onSubmit={handleSearch} style={{ position: 'relative', display: 'inline-block' }}>
        <input
          type="text"
          value={query}
          onChange={handleInputChange}
          placeholder="Search for movies..."
          style={{ padding: '8px', width: '200px', marginRight: '10px', boxSizing: 'border-box' }}
        />
        <button type="submit" style={{ padding: '8px 16px' }}>Search</button>
        {suggestions.length > 0 && (
          <ul style={{
            position: 'absolute',
            top: 'calc(100% + 2px)',
            left: '0',
            background: 'white',
            border: '1px solid #ddd',
            borderTop: 'none',
            listStyle: 'none',
            padding: '0',
            margin: '0',
            maxHeight: '200px',
            overflowY: 'auto',
            zIndex: 1000,
            width: '200px',
            boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)'
          }}>
            {suggestions.map((suggestion, index) => (
              <li
                key={index}
                onClick={() => handleSuggestionClick(suggestion)}
                style={{ padding: '8px', cursor: 'pointer' }}
              >
                {suggestion}
              </li>
            ))}
          </ul>
        )}
      </form>

      <div style={{ marginTop: '20px', display: 'flex', gap: '20px' }}>
        <select value={genre} onChange={(e) => setGenre(e.target.value)}>
          <option value="">All Genres</option>
          <option value="action">Action</option>
          <option value="drama">Drama</option>
          <option value="comedy">Comedy</option>
          <option value="thriller">Thriller</option>
          <option value="horror">Horror</option>
          <option value="Science Fiction">Sci-Fi</option>
          <option value="romance">Romance</option>
        </select>

        <input
          type="text"
          value={director}
          onChange={(e) => setDirector(e.target.value)}
          placeholder="Filter by Director"
          style={{ padding: '8px', width: '200px' }}
        />

        <input
          type="text"
          value={year}
          onChange={(e) => setYear(e.target.value)}
          placeholder="Filter by Year"
          style={{ padding: '8px', width: '100px' }}
        />
      </div>

      <div style={{ marginTop: '20px' }}>
        <h2>Your History</h2>
        {history.length > 0 ? (
          <div
            style={{
              display: 'flex',
              overflowX: 'auto',
              gap: '20px',
              paddingBottom: '10px',
              scrollbarWidth: 'thin'
            }}
          >
            {history.map((movie, index) => (
              <div
                key={movie.movieId || `history-${index}`}
                onClick={() => navigate(`/movies/${movie.movieId}`, { state: { query, genre, director, year } })}
                style={{
                  width: '200px',
                  textAlign: 'center',
                  cursor: 'pointer',
                  border: '1px solid #ddd',
                  borderRadius: '8px',
                  padding: '10px',
                  transition: 'transform 0.2s',
                  position: 'relative',
                  flexShrink: 0
                }}
              >
                <h4 style={{ fontSize: '16px', margin: '10px 0' }}>{movie.title}</h4>
                {movie.poster_path ? (
                  <img
                    src={`${baseImageUrl}${movie.poster_path}`}
                    alt={`${movie.title} Poster`}
                    style={{ width: '100%', borderRadius: '8px' }}
                  />
                ) : (
                  <div
                    style={{
                      width: '100%',
                      height: '300px',
                      backgroundColor: '#ccc',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      borderRadius: '8px'
                    }}
                  >
                    No Poster
                  </div>
                )}
                <span
                  style={{
                    position: 'absolute',
                    top: '10px',
                    right: '10px',
                    fontSize: '24px',
                    cursor: 'pointer',
                    color: favorites.has(movie.movieId) ? 'red' : 'gray'
                  }}
                  onClick={(e) => toggleFavorite(movie.movieId, e)}
                >
                  {favorites.has(movie.movieId) ? '❤️' : '🤍'}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p>No movies selected.</p>
        )}
      </div>

      {loading && <p>Loading...</p>}
      {error && <p style={{ color: 'red' }}>Error: {error}</p>}
      {movies.length === 0 && !loading && !error && query && <p>No movies found.</p>}

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '20px', marginTop: '20px' }}>
        {movies.map((movie, index) => (
          <div
            key={movie.id || `search-${index}`}
            onClick={() => {
              selectMovie(movie);
              navigate(`/movies/${movie.id}`, { state: { query, genre, director, year } });
            }}
            style={{
              width: '200px',
              textAlign: 'center',
              cursor: 'pointer',
              border: '1px solid #ddd',
              borderRadius: '8px',
              padding: '10px',
              transition: 'transform 0.2s',
              position: 'relative'
            }}
          >
            <h3 style={{ fontSize: '16px', margin: '10px 0' }}>{movie.title}</h3>
            {movie.poster_path ? (
              <img
                src={`${baseImageUrl}${movie.poster_path}`}
                alt={`${movie.title} Poster`}
                style={{ width: '100%', borderRadius: '8px' }}
              />
            ) : (
              <div
                style={{
                  width: '100%',
                  height: '300px',
                  backgroundColor: '#ccc',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: '8px'
                }}
              >
                No Poster
              </div>
            )}
            <span
              style={{
                position: 'absolute',
                top: '10px',
                right: '10px',
                fontSize: '24px',
                cursor: 'pointer',
                color: favorites.has(movie.id) ? 'red' : 'gray'
              }}
              onClick={(e) => toggleFavorite(movie.id, e)}
            >
              {favorites.has(movie.id) ? '❤️' : '🤍'}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default MovieGallery;