import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

const MovieDetails = () => {
  const [movieDetails, setMovieDetails] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const { movieId } = useParams();
  const navigate = useNavigate();
  const baseImageUrl = 'https://image.tmdb.org/t/p/w500';
  const backendUrl = 'http://localhost:8080/api/movies';

  const fetchMovieDetails = async (id) => {
    setLoading(true);
    setError(null);
    console.log('Fetching details for movieId:', id); // Debug
    const url = `${backendUrl}/${id}`;
    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error('Failed to fetch movie details');
      const data = await response.json();
      setMovieDetails(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMovieDetails(movieId);
  }, [movieId]);

  const handleBack = () => {
    navigate('/');
  };

  return (
    <div style={{ padding: '20px', maxWidth: '1000px', margin: '0 auto' ,minHeight:"720px"}}>
      <h1>Movie Details</h1>
      {loading && <p>Loading...</p>}
      {error && <p style={{ color: 'red' }}>Error: {error}</p>}
      {movieDetails && (
        <div
          style={{
            border: '1px solid #ddd',
            borderRadius: '8px',
            padding: '20px',
            backgroundColor: '#f9f9f9',
            minHeight: '400px',
          }}
        >
          <button
            onClick={handleBack}
            style={{ float: 'right', padding: '5px 10px', cursor: 'pointer' }}
          >
            Back to Gallery
          </button>
          <h2>{movieDetails.title}</h2>
          {movieDetails.poster_path && (
            <img
              src={`${baseImageUrl}${movieDetails.poster_path}`}
              alt={`${movieDetails.title} Poster`}
              style={{ width: '200px', borderRadius: '8px', float: 'left', marginRight: '20px'}}
            />
          )}
          <p><strong>Release Date:</strong> {movieDetails.release_date || 'N/A'}</p>
          <p><strong>Overview:</strong> {movieDetails.overview || 'No overview available.'}</p>
          <p>
            <strong>Genres:</strong>{' '}
            {movieDetails.genres ? movieDetails.genres.map((g) => g.name).join(', ') : 'N/A'}
          </p>
          <p><strong>Runtime:</strong> {movieDetails.runtime ? `${movieDetails.runtime} min` : 'N/A'}</p>
          <p><strong>Rating:</strong> {movieDetails.vote_average || 'N/A'} / 10</p>
        </div>
      )}
    </div>
  );
};

export default MovieDetails;