import React, { useState, useEffect } from 'react';
<<<<<<< HEAD
import { useNavigate, useLocation } from 'react-router-dom';

const MovieGallery = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const initialState = location.state || {
    query: '',
    sortOption: 'popularity.desc',
    genre: '',
    rating: 0,
    director: '',
  };

  const [query, setQuery] = useState(initialState.query);
=======
import { useNavigate } from 'react-router-dom';

const MovieGallery = ({ auth0Client, isAuthenticated }) => {
  const [query, setQuery] = useState('');
>>>>>>> 267d0d5 (Updated movie search project with frontend and backend changes)
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [sortOption, setSortOption] = useState(initialState.sortOption);
  const [genre, setGenre] = useState(initialState.genre);
  const [rating, setRating] = useState(initialState.rating);
  const [director, setDirector] = useState(initialState.director);
  const [favorites, setFavorites] = useState(new Set());

  const baseImageUrl = 'https://image.tmdb.org/t/p/w500';
  const backendUrl = 'http://localhost:8080/api/movies';

<<<<<<< HEAD
  // Fetch movies only when sorting/filtering changes
  useEffect(() => {
    fetchMovies();
  }, [sortOption, genre, rating, director]);

  const fetchMovies = async () => {
=======
  const fetchMovies = async (searchQuery) => {
    if (!searchQuery || !isAuthenticated || !auth0Client) {
      setMovies([]);
      return;
    }
>>>>>>> 267d0d5 (Updated movie search project with frontend and backend changes)
    setLoading(true);
    setError(null);

const url = `${backendUrl}/search?query=${encodeURIComponent(query)}&sort=${encodeURIComponent(sortOption)}&genre=${encodeURIComponent(genre)}&rating=${encodeURIComponent(Number(rating))}&director=${encodeURIComponent(director)}`;

    try {
      const token = await auth0Client.getTokenSilently();
      const response = await fetch(url, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!response.ok) throw new Error('Failed to fetch movies from backend');
      const data = await response.json();
      setMovies(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

<<<<<<< HEAD
  const toggleFavorite = (movieId, event) => {
    event.stopPropagation();
    setFavorites((prevFavorites) => {
      const updatedFavorites = new Set(prevFavorites);
      if (updatedFavorites.has(movieId)) {
        updatedFavorites.delete(movieId);
      } else {
        updatedFavorites.add(movieId);
      }
      return updatedFavorites;
    });
=======
  const handleSearch = (e) => {
    e.preventDefault();
    fetchMovies(query);
  };

  const handleMovieClick = (movieId) => {
    navigate(`/movies/${movieId}`);
>>>>>>> 267d0d5 (Updated movie search project with frontend and backend changes)
  };

  useEffect(() => {
    if (!isAuthenticated && auth0Client) {
      auth0Client.loginWithRedirect();
    }
  }, [isAuthenticated, auth0Client]);

  if (!isAuthenticated) return null;

  return (
    <div style={{ padding: '20px' }}>
      <h1>Movie Gallery</h1>

      {/* Search Form */}
      <form onSubmit={(e) => { e.preventDefault(); fetchMovies(); }}>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search for movies..."
          style={{ padding: '8px', width: '200px', marginRight: '10px' }}
        />
        <button type="submit" style={{ padding: '8px 16px' }}>Search</button>
      </form>

      {/* Sorting & Filtering Options */}
      <div style={{ marginTop: '20px', display: 'flex', gap: '20px' }}>
        <select value={sortOption} onChange={(e) => setSortOption(e.target.value)}>
          <option value="popularity.desc">Sort by Popularity (High to Low)</option>
          <option value="popularity.asc">Sort by Popularity (Low to High)</option>
          <option value="release_date.desc">Newest First</option>
          <option value="release_date.asc">Oldest First</option>
          <option value="vote_average.desc">Rating (High to Low)</option>
          <option value="vote_average.asc">Rating (Low to High)</option>
        </select>

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

        <label>Minimum Rating:</label>
        <select value={rating} onChange={(e) => setRating(e.target.value)}>
          {[...Array(11).keys()].map((num) => (
            <option key={num} value={num}>{num}</option>
          ))}
        </select>
      </div>

      {/* Loading & Error Messages */}
      {loading && <p>Loading...</p>}
      {error && <p style={{ color: 'red' }}>Error: {error}</p>}
      {movies.length === 0 && !loading && !error && query && <p>No movies found.</p>}

      {/* Movie List */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '20px', marginTop: '20px' }}>
        {movies.map((movie) => (
          <div
            key={movie.id}
<<<<<<< HEAD
=======
            onClick={() => handleMovieClick(movie.id)}
>>>>>>> 267d0d5 (Updated movie search project with frontend and backend changes)
            style={{
              width: '200px',
              textAlign: 'center',
              cursor: 'pointer',
              border: '1px solid #ddd',
              borderRadius: '8px',
              padding: '10px',
              transition: 'transform 0.2s',
              position: 'relative',
            }}
            onClick={() => navigate(`/movies/${movie.id}`, { state: { query, sortOption, genre, rating, director } })}
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

            {/* Favorite Button */}
            <span
              style={{
                position: 'absolute',
                top: '10px',
                right: '10px',
                fontSize: '24px',
                cursor: 'pointer',
                color: favorites.has(movie.id) ? 'red' : 'gray',
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
