package ar.edu.utn.frc.tup.piii.engine.action;

import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;

import java.util.List;

public class ActionResult {
    private boolean success;
    private String clientRequestId;
    private Object publicState;
    private Object privateState;
    private List<GameEvent> events;
    private GameError error;

    public ActionResult() {}

    public ActionResult(boolean success,
                        String clientRequestId,
                        Object publicState,
                        Object privateState,
                        List<GameEvent> events,
                        GameError error) {
        this.success = success;
        this.clientRequestId = clientRequestId;
        this.publicState = publicState;
        this.privateState = privateState;
        this.events = events;
        this.error = error;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }

    public Object getPublicState() { return publicState; }
    public void setPublicState(Object publicState) { this.publicState = publicState; }

    public Object getPrivateState() { return privateState; }
    public void setPrivateState(Object privateState) { this.privateState = privateState; }

    public List<GameEvent> getEvents() { return events; }
    public void setEvents(List<GameEvent> events) { this.events = events; }

    public GameError getError() { return error; }
    public void setError(GameError error) { this.error = error; }
}
