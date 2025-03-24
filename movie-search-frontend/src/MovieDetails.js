import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';

const MovieDetails = ({ auth0Client, isAuthenticated }) => {
  const { movieId } = useParams();
  const [movie, setMovie] = useState(null);
  const [trailerKey, setTrailerKey] = useState(null); // Separate state for trailer
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isHovering, setIsHovering] = useState(false);
  const navigate = useNavigate();
<<<<<<< HEAD
  const location = useLocation();
  const baseImageUrl = 'https://image.tmdb.org/t/p/w500';
  const backendUrl = 'http://localhost:8080/api/movies';

  useEffect(() => {
    fetchMovieDetails(movieId);
  }, [movieId]);

  const fetchMovieDetails = async (id) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${backendUrl}/${id}`);
      if (!response.ok) throw new Error('Failed to fetch movie details');
      const data = await response.json();
      setMovieDetails(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    navigate('/', { state: location.state });
=======

  const baseImageUrl = 'https://image.tmdb.org/t/p/w500';
  const baseVideoUrl = 'https://www.youtube.com/embed/';
  const tmdbApiKey = '0ecbd29e4e03cdcfccd72d76ba826345'; // Replace with your TMDB API key

  useEffect(() => {
    const fetchMovieDetails = async () => {
      if (!isAuthenticated || !auth0Client) {
        if (auth0Client) auth0Client.loginWithRedirect();
        return;
      }

      setLoading(true);
      setError(null);

      try {
        // Fetch movie details from your backend
        const token = await auth0Client.getTokenSilently();
        const movieResponse = await fetch(`http://localhost:8080/api/movies/${movieId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!movieResponse.ok) {
          throw new Error(`HTTP error! Status: ${movieResponse.status}`);
        }

        const movieData = await movieResponse.json();
        console.log('Movie Data from Backend:', movieData);

        // Fetch trailer directly from TMDB
        const videoResponse = await fetch(
          `https://api.themoviedb.org/3/movie/${movieId}/videos?api_key=${tmdbApiKey}`
        );
        if (!videoResponse.ok) {
          throw new Error(`TMDB video fetch error! Status: ${videoResponse.status}`);
        }

        const videoData = await videoResponse.json();
        console.log('Video Data from TMDB:', videoData);
        const trailer = videoData.results?.find(
          video => video.type === 'Trailer' && video.site === 'YouTube'
        )?.key;
        setTrailerKey(trailer);

        setMovie(movieData);
      } catch (err) {
        console.error('Error fetching movie details:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchMovieDetails();
  }, [movieId, auth0Client, isAuthenticated]);

  const handleBackClick = () => {
    navigate('/');
>>>>>>> 267d0d5 (Updated movie search project with frontend and backend changes)
  };

  const handleMouseEnter = () => {
    console.log('Hovering, trailer:', trailerKey);
    setIsHovering(true);
  };
  const handleMouseLeave = () => {
    console.log('Leaving hover');
    setIsHovering(false);
  };

  if (!isAuthenticated) return null;
  if (loading) return <div style={{ padding: '20px' }}>Loading...</div>;
  if (error) return (
    <div style={{ padding: '20px' }}>
      <p style={{ color: 'red' }}>Error: {error}</p>
      <button onClick={handleBackClick} style={buttonStyle}>Back to Gallery</button>
    </div>
  );

  return (
<<<<<<< HEAD
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
=======
    <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
      <button onClick={handleBackClick} style={buttonStyle}>Back to Gallery</button>
      <h1 style={{ fontSize: '28px', margin: '20px 0' }}>{movie.title}</h1>
      <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
        <div
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
          style={{ position: 'relative', width: '300px' }}
        >
          {isHovering && trailerKey ? (
            <iframe
              src={`${baseVideoUrl}${trailerKey}?autoplay=1&mute=1`}
              title={`${movie.title} Trailer`}
              style={{ width: '300px', height: '450px', borderRadius: '8px', border: 'none' }}
              allow="autoplay; encrypted-media"
            />
          ) : isHovering && !trailerKey ? (
            <div
              style={{
                width: '300px',
                height: '450px',
                backgroundColor: '#000',
                color: 'white',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: '8px',
              }}
            >
              No Trailer Available
            </div>
          ) : movie.poster_path ? (
            <img
              src={`${baseImageUrl}${movie.poster_path}`}
              alt={`${movie.title} Poster`}
              style={{ width: '300px', borderRadius: '8px' }}
            />
          ) : (
            <div
              style={{
                width: '300px',
                height: '450px',
                backgroundColor: '#ccc',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: '8px',
              }}
            >
              No Poster Available
            </div>
          )}
        </div>
        <div style={{ flex: 1, minWidth: '300px' }}>
          <p><strong>Release Year:</strong> {movie.release_date?.substring(0, 4) || 'N/A'}</p>
          <p><strong>Overview:</strong> {movie.overview || 'No description available.'}</p>
          {movie.runtime && <p><strong>Runtime:</strong> {movie.runtime} minutes</p>}
          {movie.genres && (
            <p><strong>Genres:</strong> {movie.genres.map(g => g.name).join(', ') || 'N/A'}</p>
          )}
          {movie.director && <p><strong>Director:</strong> {movie.director}</p>}
          {movie.production_companies && (
            <p><strong>Production:</strong> {movie.production_companies.map(c => c.name).join(', ') || 'N/A'}</p>
          )}
          {movie.vote_average && <p><strong>Rating:</strong> {movie.vote_average}/10</p>}
          {movie.cast && (
            <p><strong>Cast:</strong> {movie.cast.map(c => c.name).join(', ') || 'N/A'}</p>
          )}
        </div>
      </div>
    </div>
  );
};

const buttonStyle = {
  padding: '8px 16px',
  backgroundColor: '#007bff',
  color: 'white',
  border: 'none',
  borderRadius: '4px',
  cursor: 'pointer',
  marginBottom: '20px',
};

export default MovieDetails;
>>>>>>> 267d0d5 (Updated movie search project with frontend and backend changes)
