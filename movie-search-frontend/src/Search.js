import React, { useState } from 'react';

function Search() {
  const [query, setQuery] = useState('');
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const debounce = (func, delay) => {
    let timeoutId;
    return (...args) => {
      if (timeoutId) {
        console.log('Debouncing: Cleared timeout', timeoutId);
        clearTimeout(timeoutId);
      }
      timeoutId = setTimeout(() => {
        console.log('Debouncing: Executing after delay', args);
        func(...args);
      }, delay);
      console.log('Debouncing: Set new timeout', timeoutId);
    };
  };

  const fetchMovies = async (searchQuery) => {
    if (!searchQuery) {
      console.log('Query is empty, resetting movies');
      setMovies([]);
      setError(null);
      return;
    }
    console.log('Fetching movies for query:', searchQuery);
    setLoading(true);
    setError(null);
    try {
      const url = `http://localhost:8080/api/movies/search?query=${encodeURIComponent(searchQuery)}`;
      console.log('Request URL:', url);
      const response = await fetch(url);
      console.log('Response status:', response.status);
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      const data = await response.json();
      console.log('Fetched data:', data);
      // Log movie titles to verify fuzzy search
      const titles = data.map(movie => movie.title);
      console.log('Movie titles received:', titles);
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
    console.log('Input changed to:', value);
    setQuery(value);
    debouncedFetchMovies(value);
  };

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col items-center p-4">
      <h1 className="text-3xl font-bold text-gray-800 mb-6">Movie Search</h1>
      <div className="w-full max-w-md">
        <input
          type="text"
          value={query}
          onChange={handleInputChange}
          placeholder="Search for a movie..."
          className="w-full p-3 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      {loading && <p className="mt-4 text-gray-600">Loading...</p>}
      {error && <p className="mt-4 text-red-600">Error: {error}</p>}
      <div className="mt-6 w-full max-w-2xl">
        {movies.length > 0 ? (
          <ul className="space-y-4">
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
        ) : (
          query && !loading && !error && <p className="mt-4 text-gray-600">No movies found.</p>
        )}
      </div>
    </div>
  );
}

export default Search;