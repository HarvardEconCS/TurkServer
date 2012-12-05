package edu.harvard.econcs.turkserver.schema;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QWorker is a Querydsl query type for Worker
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QWorker extends com.mysema.query.sql.RelationalPathBase<Worker> {

    private static final long serialVersionUID = -190560437;

    public static final QWorker worker = new QWorker("worker");

    public final StringPath id = createString("id");

    public final StringPath notify = createString("notify");

    public final com.mysema.query.sql.PrimaryKey<Worker> primary = createPrimaryKey(id);

    public final com.mysema.query.sql.ForeignKey<Session> _session3Fk = createInvForeignKey(id, "workerId");

    public final com.mysema.query.sql.ForeignKey<Quiz> _quizIbfk2 = createInvForeignKey(id, "workerId");

    public final com.mysema.query.sql.ForeignKey<Qual> _qual1Fk = createInvForeignKey(id, "workerId");

    public QWorker(String variable) {
        super(Worker.class, forVariable(variable), "null", "worker");
    }

    @SuppressWarnings("all")
    public QWorker(Path<? extends Worker> path) {
        super((Class)path.getType(), path.getMetadata(), "null", "worker");
    }

    public QWorker(PathMetadata<?> metadata) {
        super(Worker.class, metadata, "null", "worker");
    }

}

