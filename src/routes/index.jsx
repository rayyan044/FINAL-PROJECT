import { createFileRoute, Link } from "@tanstack/react-router";
import { FiUser, FiLock, FiArrowRight, FiTruck, FiBarChart2, FiShield } from "react-icons/fi";
import { SiteNav } from "../components/SiteNav";
import "../styles/landing.css";
export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "FEFTMS — Falcon Energy Fuel Trading Management System" },
      {
        name: "description",
        content:
          "Digital fuel trading platform for managing customer requests, fuel loading, delivery operations, driver tracking and invoicing.",
      },
      { property: "og:title", content: "FEFTMS — Falcon Energy" },
      { property: "og:description", content: "Enterprise fuel trading management platform." },
    ],
  }),
  component: Landing,
});
function Landing() {
  return (
    <div className="fef-landing">
      <SiteNav />

      <section className="fef-hero">
        <div className="fef-hero-overlay" />
        <div className="fef-hero-inner fef-fade-in">
          <span className="fef-hero-eyebrow">Falcon Energy</span>
          <h1 className="fef-hero-title">
            Falcon Energy Fuel Trading <span>Management System</span>
          </h1>
          <p className="fef-hero-sub">
            Digital Fuel Trading Platform for Managing Customer Requests, Fuel Loading, Delivery
            Operations, Driver Tracking and Invoicing.
          </p>

          <div className="fef-portal-cards">
            <div className="fef-portal-card fef-slide-up">
              <div className="fef-portal-icon fef-portal-icon-accent">
                <FiUser size={28} />
              </div>
              <h3>Customer Portal</h3>
              <p>Submit fuel requests online quickly and track your orders.</p>
              <Link to="/customer" className="fef-btn fef-btn-accent fef-btn-block">
                Continue as Customer <FiArrowRight />
              </Link>
            </div>

            <div className="fef-portal-card fef-slide-up" style={{ animationDelay: ".1s" }}>
              <div className="fef-portal-icon fef-portal-icon-primary">
                <FiLock size={28} />
              </div>
              <h3>User Portal</h3>
              <p>Secure login for company staff and operations team.</p>
              <Link to="/login" className="fef-btn fef-btn-primary fef-btn-block">
                Login <FiArrowRight />
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section id="about" className="fef-features">
        <div className="fef-features-inner">
          <h2>One platform. End-to-end fuel operations.</h2>
          <p className="fef-section-sub">
            From customer request to delivery confirmation — FEFTMS keeps your trading operation
            moving with clarity and control.
          </p>
          <div className="fef-feature-grid">
            <div className="fef-feature">
              <FiTruck size={26} />
              <h4>Loading & Dispatch</h4>
              <p>Coordinate loading officers, trucks and drivers in real time.</p>
            </div>
            <div className="fef-feature">
              <FiBarChart2 size={26} />
              <h4>Sales & Invoicing</h4>
              <p>Generate PFIs, approve requests and track revenue across customers.</p>
            </div>
            <div className="fef-feature">
              <FiShield size={26} />
              <h4>Role-Based Access</h4>
              <p>Dedicated dashboards for admins, sales, ops, loading and drivers.</p>
            </div>
          </div>
        </div>
      </section>

      <footer id="contact" className="fef-footer">
        <div className="fef-footer-inner">
          <div>
            <strong>Falcon Energy</strong>
            <p>Fuel Trading Management System</p>
          </div>
          <div className="fef-footer-meta">
            © {new Date().getFullYear()} Falcon Energy. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}
