package edu.harvard.econcs.turkserver.schema;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QRound is a Querydsl query type for Round
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QRound extends com.mysema.query.sql.RelationalPathBase<Round> {

    private static final long serialVersionUID = 1651806241;

    public static final QRound round = new QRound("round");

    public final DateTimePath<java.sql.Timestamp> endTime = createDateTime("endTime", java.sql.Timestamp.class);

    public final StringPath experimentId = createString("experimentId");

    public final StringPath results = createString("results");

    public final NumberPath<Integer> roundnum = createNumber("roundnum", Integer.class);

    public final DateTimePath<java.sql.Timestamp> startTime = createDateTime("startTime", java.sql.Timestamp.class);

    public final com.mysema.query.sql.PrimaryKey<Round> primary = createPrimaryKey(experimentId, roundnum);

    public QRound(String variable) {
        super(Round.class, forVariable(variable), "null", "round");
    }

    @SuppressWarnings("all")
    public QRound(Path<? extends Round> path) {
        super((Class)path.getType(), path.getMetadata(), "null", "round");
    }

    public QRound(PathMetadata<?> metadata) {
        super(Round.class, metadata, "null", "round");
    }

}

