import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const MovieGallery = () => {
  const [query, setQuery] = useState('');
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const baseImageUrl = 'https://image.tmdb.org/t/p/w500';
  const backendUrl = 'http://localhost:8080/api/movies';
  const navigate = useNavigate();

  const fetchMovies = async (searchQuery) => {
    if (!searchQuery) {
      setMovies([]);
      return;
    }
    setLoading(true);
    setError(null);
    const url = `${backendUrl}/search?query=${encodeURIComponent(searchQuery)}`;
    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error('Failed to fetch movies from backend');
      const data = await response.json();
      setMovies(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchMovies(query);
  };

  const handleMovieClick = (movieId) => {
    console.log('Navigating to:', `/movies/${movieId}`); // Debug
    navigate(`/movies/${movieId}`); // Pass only the ID
  };

  return (
    <div style={{ padding: '20px' }}>
      <h1>Movie Gallery</h1>
      <form onSubmit={handleSearch}>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search for movies..."
          style={{ padding: '8px', width: '200px', marginRight: '10px' }}
        />
        <button type="submit" style={{ padding: '8px 16px' }}>Search</button>
      </form>

      {loading && <p>Loading...</p>}
      {error && <p style={{ color: 'red' }}>Error: {error}</p>}
      {movies.length === 0 && !loading && !error && query && <p>No movies found.</p>}

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '20px', marginTop: '20px' }}>
        {movies.map((movie) => (
          <div
            key={movie.id}
            onClick={() => handleMovieClick(movie.id)} // Pass movie.id
            style={{
              width: '200px',
              textAlign: 'center',
              cursor: 'pointer',
              border: '1px solid #ddd',
              borderRadius: '8px',
              padding: '10px',
              transition: 'transform 0.2s',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.transform = 'scale(1.05)')}
            onMouseLeave={(e) => (e.currentTarget.style.transform = 'scale(1)')}
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
                  borderRadius: '8px',
                }}
              >
                No Poster
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default MovieGallery;