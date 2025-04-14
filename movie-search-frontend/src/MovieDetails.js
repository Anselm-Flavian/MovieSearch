import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';

const MovieDetails = ({ auth0Client, isAuthenticated }) => {
  const { movieId } = useParams();
  const [movie, setMovie] = useState(null);
  const [trailerKey, setTrailerKey] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isHovering, setIsHovering] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const playerRef = useRef(null);
  const playerInitialized = useRef(false);

  const baseImageUrl = 'https://image.tmdb.org/t/p/w500';
  const baseVideoUrl = 'https://www.youtube.com/embed/';
  const backendUrl = 'http://localhost:8080/api/movies';
  const tmdbApiKey = '0ecbd29e4e03cdcfccd72d76ba826345';

  useEffect(() => {
    const fetchMovieDetails = async () => {
      if (!isAuthenticated || !auth0Client) {
        if (auth0Client) auth0Client.loginWithRedirect();
        return;
      }

      setLoading(true);
      setError(null);

      try {
        const token = await auth0Client.getTokenSilently();
        const movieResponse = await fetch(`${backendUrl}/${movieId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!movieResponse.ok) {
          throw new Error(`HTTP error! Status: ${movieResponse.status}`);
        }

        const movieData = await movieResponse.json();
        setMovie(movieData);

        const videoResponse = await fetch(
          `https://api.themoviedb.org/3/movie/${movieId}/videos?api_key=${tmdbApiKey}`
        );

        if (!videoResponse.ok) {
          throw new Error(`TMDB video fetch error! Status: ${videoResponse.status}`);
        }

        const videoData = await videoResponse.json();
        const trailer = videoData.results?.find(
          video => video.type === 'Trailer' && video.site === 'YouTube'
        )?.key;

        setTrailerKey(trailer);
      } catch (err) {
        console.error('Error fetching movie details:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchMovieDetails();
  }, [movieId, auth0Client, isAuthenticated]);

  // Load YouTube IFrame API
  useEffect(() => {
    if (!trailerKey || playerInitialized.current) return;

    const onYouTubeIframeAPIReady = () => {
      playerRef.current = new window.YT.Player(`youtube-player-${movieId}`, {
        videoId: trailerKey,
        events: {
          onReady: () => {
            playerInitialized.current = true;
          },
        },
      });
    };

    if (!window.YT) {
      const tag = document.createElement('script');
      tag.src = 'https://www.youtube.com/iframe_api';
      const firstScriptTag = document.getElementsByTagName('script')[0];
      firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
      window.onYouTubeIframeAPIReady = onYouTubeIframeAPIReady;
    } else {
      onYouTubeIframeAPIReady();
    }
  }, [trailerKey, movieId]);

  const handleBackClick = () => {
    navigate('/results', { state: location.state });
  };

  const handleMouseEnter = () => {
    setIsHovering(true);
    if (playerRef.current && playerInitialized.current) {
      playerRef.current.playVideo();
    }
  };

  const handleMouseLeave = () => {
    setIsHovering(false);
    if (playerRef.current && playerInitialized.current) {
      playerRef.current.pauseVideo();
    }
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
    <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
      <button onClick={handleBackClick} style={buttonStyle}>Back to Gallery</button>
      <h1 style={{ fontSize: '28px', margin: '20px 0' }}>{movie.title}</h1>
      <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
        <div
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
          style={{ position: 'relative', width: '300px', height: '450px' }}
        >
          {trailerKey ? (
            <div
              id={`youtube-player-${movieId}`}
              style={{ width: '100%', height: '100%', borderRadius: '8px', overflow: 'hidden' }}
            ></div>
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
