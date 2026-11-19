import { fetchAll } from '../../api/pages';
import { targetsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';

/** Every active target with its progress (targets are few: tens per month). */
export const useAllTargets = () => useLoad(() => fetchAll((page, size) => targetsApi.list({ status: 'ACTIVE', page, size })), []);
