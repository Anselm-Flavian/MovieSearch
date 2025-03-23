import React from 'react';

const MovieCard = ({ movie, onClick }) => {
  const baseImageUrl = 'https://image.tmdb.org/t/p/w500';

  return (
    <div
      onClick={() => onClick(movie.id)}
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
  );
};

export default MovieCard;
