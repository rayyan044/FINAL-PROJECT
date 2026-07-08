import { Link } from "@tanstack/react-router";
import "../styles/site-nav.css";
export function SiteNav() {
  return (
    <header className="fef-sitenav">
      <div className="fef-sitenav-inner">
        <Link to="/" className="fef-brand">
          <span className="fef-brand-mark">F</span>
          <span className="fef-brand-text">
            Falcon <span>Energy</span>
          </span>
        </Link>
        <nav className="fef-sitenav-links">
          <Link to="/" activeOptions={{ exact: true }} activeProps={{ className: "active" }}>
            Home
          </Link>
          <a href="#about">About</a>
          <a href="#contact">Contact</a>
        </nav>
        <div className="fef-sitenav-cta">
          <Link to="/login" className="fef-btn fef-btn-outline">
            Staff Login
          </Link>
        </div>
      </div>
    </header>
  );
}
