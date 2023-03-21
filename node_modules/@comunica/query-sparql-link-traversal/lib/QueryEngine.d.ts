import { QueryEngineBase } from '@comunica/actor-init-query';
import type { ActorInitQueryBase } from '@comunica/actor-init-query';
import type { IQueryContextCommon, QueryAlgebraContext, QueryStringContext, SourceType } from '@comunica/types';
/**
 * A Comunica SPARQL query engine.
 */
export declare class QueryEngine extends QueryEngineBase<IQueryContextCommon, Omit<QueryStringContext, 'sources'> & {
    sources?: SourceType[];
}, Omit<QueryAlgebraContext, 'sources'> & {
    sources?: SourceType[];
}> {
    constructor(engine?: ActorInitQueryBase);
}
