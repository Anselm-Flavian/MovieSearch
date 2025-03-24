import React, { useState, useEffect } from 'react';

function Search({ auth0Client, isAuthenticated }) {
  const [query, setQuery] = useState('');
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const debounce = (func, delay) => {
    let timeoutId;
    return (...args) => {
      if (timeoutId) clearTimeout(timeoutId);
      timeoutId = setTimeout(() => func(...args), delay);
    };
  };

  const fetchMovies = async (searchQuery) => {
    if (!searchQuery || !isAuthenticated || !auth0Client) {
      setMovies([]);
      setError(null);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const token = await auth0Client.getTokenSilently();
      const url = `http://localhost:8080/api/movies/search?query=${encodeURIComponent(searchQuery)}`;
      const response = await fetch(url, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      const data = await response.json();
      setMovies(data || []);
    } catch (error) {
      console.error('Error fetching movies:', error);
      setError(error.message);
      setMovies([]);
    } finally {
      setLoading(false);
    }
  };

  const debouncedFetchMovies = debounce(fetchMovies, 500);

  const handleInputChange = (e) => {
    const value = e.target.value;
    setQuery(value);
    debouncedFetchMovies(value);
  };

  useEffect(() => {
    if (!isAuthenticated && auth0Client) {
      auth0Client.loginWithRedirect();
    }
  }, [isAuthenticated, auth0Client]);

  if (!isAuthenticated) return null;

  return (
    <div className="search-container">
      <input
        type="text"
        value={query}
        onChange={handleInputChange}
        placeholder="Search for a movie..."
        className="w-full p-3 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
      {loading && <p className="mt-4 text-gray-600">Loading...</p>}
      {error && <p className="mt-4 text-red-600">Error: {error}</p>}
      {movies.length > 0 && (
        <ul className="space-y-4 mt-4">
          {movies.map((movie) => (
            <li
              key={movie.id}
              className="p-4 bg-white rounded-lg shadow-md hover:shadow-lg transition"
            >
              <h2 className="text-xl font-semibold text-gray-800">{movie.title}</h2>
              <p className="text-gray-600">{movie.release_date?.substring(0, 4) || 'N/A'}</p>
              <p className="text-gray-500">{movie.overview?.substring(0, 100)}...</p>
            </li>
          ))}
        </ul>
      )}
      {query && !loading && !error && movies.length === 0 && (
        <p className="mt-4 text-gray-600">No movies found.</p>
      )}
    </div>
  );
}

export default Search;