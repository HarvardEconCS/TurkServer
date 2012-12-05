package edu.harvard.econcs.turkserver.schema;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QSession is a Querydsl query type for Session
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QSession extends com.mysema.query.sql.RelationalPathBase<Session> {

    private static final long serialVersionUID = -1152579479;

    public static final QSession session = new QSession("session");

    public final StringPath assignmentId = createString("assignmentId");

    public final NumberPath<java.math.BigDecimal> bonusPaid = createNumber("bonusPaid", java.math.BigDecimal.class);

    public final StringPath comment = createString("comment");

    public final StringPath experimentId = createString("experimentId");

    public final StringPath hitId = createString("hitId");

    public final StringPath hitStatus = createString("hitStatus");

    public final StringPath inactiveData = createString("inactiveData");

    public final NumberPath<Double> inactivePercent = createNumber("inactivePercent", Double.class);

    public final StringPath ipAddr = createString("ipAddr");

    public final DateTimePath<java.sql.Timestamp> lobbyTime = createDateTime("lobbyTime", java.sql.Timestamp.class);

    public final NumberPath<Integer> numDisconnects = createNumber("numDisconnects", Integer.class);

    public final NumberPath<java.math.BigDecimal> paid = createNumber("paid", java.math.BigDecimal.class);

    public final StringPath setId = createString("setId");

    public final StringPath username = createString("username");

    public final StringPath workerId = createString("workerId");

    public final com.mysema.query.sql.PrimaryKey<Session> primary = createPrimaryKey(hitId);

    public final com.mysema.query.sql.ForeignKey<Worker> session3Fk = createForeignKey(workerId, "id");

    public final com.mysema.query.sql.ForeignKey<Experiment> session1Fk = createForeignKey(experimentId, "id");

    public final com.mysema.query.sql.ForeignKey<Sets> session2Fk = createForeignKey(setId, "name");

    public QSession(String variable) {
        super(Session.class, forVariable(variable), "null", "session");
    }

    @SuppressWarnings("all")
    public QSession(Path<? extends Session> path) {
        super((Class)path.getType(), path.getMetadata(), "null", "session");
    }

    public QSession(PathMetadata<?> metadata) {
        super(Session.class, metadata, "null", "session");
    }

}

