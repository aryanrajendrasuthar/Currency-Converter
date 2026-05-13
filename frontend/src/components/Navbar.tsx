import { Link, useNavigate, useLocation } from 'react-router-dom';
import { TrendingUp, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { isAuthenticated, userName, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => { logout(); navigate('/login'); };

  const navItems = [
    { path: '/', label: 'Converter' },
    { path: '/history', label: 'History', protected: true },
    { path: '/alerts', label: 'Alerts', protected: true },
  ];

  return (
    <nav className="navbar">
      <Link to="/" className="navbar-brand">
        <TrendingUp size={22} />
        CurrEx
      </Link>

      <div className="navbar-links">
        {navItems.filter(i => !i.protected || isAuthenticated).map(({ path, label }) => (
          <Link
            key={path}
            to={path}
            className={`nav-link ${location.pathname === path ? 'active' : ''}`}
          >
            {label}
          </Link>
        ))}
      </div>

      <div className="navbar-end">
        {isAuthenticated ? (
          <div className="navbar-user">
            <span className="user-name">{userName}</span>
            <button className="btn-logout" onClick={handleLogout}>
              <LogOut size={15} /> Logout
            </button>
          </div>
        ) : (
          <>
            <Link to="/login" className="btn-nav">Sign in</Link>
            <Link to="/register" className="btn-nav gold">Register</Link>
          </>
        )}
      </div>
    </nav>
  );
}
