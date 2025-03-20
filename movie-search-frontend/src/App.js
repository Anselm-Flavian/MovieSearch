import logo from './logo.svg';
import './App.css';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Search from './Search';
import MovieDetails from './MovieDetails';
import MovieGallery from './MovieGallery';

function App() {
  return (
    <div className="App">
      <Router>
        <Search /> {/* Always visible, e.g., in a header */}
        <Routes>
          <Route path="/" element={<MovieGallery />} />
          <Route path="/movies/:movieId" element={<MovieDetails />} />
        </Routes>
      </Router>
    </div>
  );
}

export default App;