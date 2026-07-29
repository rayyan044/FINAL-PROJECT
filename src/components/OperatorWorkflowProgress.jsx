const STAGES = ["Loading", "Documentation", "Dispatch", "Delivery", "Completed"];

/** A visual guide only; it does not alter workflow state or permissions. */
export function OperatorWorkflowProgress({ current, nextLabel, onNext }) {
  const currentIndex = STAGES.indexOf(current);
  return (
    <div className="operator-workflow" aria-label="Operator workflow progress">
      <div className="operator-workflow-stages">
        {STAGES.map((stage, index) => (
          <div
            className={`operator-workflow-stage ${index < currentIndex ? "is-complete" : ""} ${index === currentIndex ? "is-current" : ""}`}
            key={stage}
          >
            <span>{index + 1}</span>
            <small>{stage}</small>
          </div>
        ))}
      </div>
      {nextLabel && onNext && (
        <button className="fef-btn fef-btn-outline operator-workflow-next" onClick={onNext}>
          Next Step: {nextLabel}
        </button>
      )}
    </div>
  );
}
