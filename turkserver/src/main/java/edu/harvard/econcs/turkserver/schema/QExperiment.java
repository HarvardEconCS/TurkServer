package edu.harvard.econcs.turkserver.schema;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QExperiment is a Querydsl query type for Experiment
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QExperiment extends com.mysema.query.sql.RelationalPathBase<Experiment> {

    private static final long serialVersionUID = 21176650;

    public static final QExperiment experiment = new QExperiment("experiment");

    public final DateTimePath<java.sql.Timestamp> endTime = createDateTime("endTime", java.sql.Timestamp.class);

    public final StringPath id = createString("id");

    public final StringPath inputdata = createString("inputdata");

    public final NumberPath<Integer> participants = createNumber("participants", Integer.class);

    public final StringPath results = createString("results");

    public final StringPath setId = createString("setId");

    public final DateTimePath<java.sql.Timestamp> startTime = createDateTime("startTime", java.sql.Timestamp.class);

    public final com.mysema.query.sql.PrimaryKey<Experiment> primary = createPrimaryKey(id);

    public final com.mysema.query.sql.ForeignKey<Sets> experimentIbfk1 = createForeignKey(setId, "name");

    public final com.mysema.query.sql.ForeignKey<Session> _session1Fk = createInvForeignKey(id, "experimentId");

    public QExperiment(String variable) {
        super(Experiment.class, forVariable(variable), "null", "experiment");
    }

    @SuppressWarnings("all")
    public QExperiment(Path<? extends Experiment> path) {
        super((Class)path.getType(), path.getMetadata(), "null", "experiment");
    }

    public QExperiment(PathMetadata<?> metadata) {
        super(Experiment.class, metadata, "null", "experiment");
    }

}

