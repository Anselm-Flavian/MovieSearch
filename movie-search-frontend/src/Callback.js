import React, { useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-spa-js';
import { useNavigate } from 'react-router-dom';

const Callback = () => {
  const { handleRedirectCallback } = useAuth0();
  const navigate = useNavigate();

  useEffect(() => {
    const handleAuth = async () => {
      try {
        await handleRedirectCallback();
        navigate('/'); // Redirect to home after login
      } catch (error) {
        console.error('Error handling callback:', error);
        navigate('/');
      }
    };
    handleAuth();
  }, [handleRedirectCallback, navigate]);

  return <div>Loading...</div>;
};

export default Callback;