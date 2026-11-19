import 'package:material_ui/material_ui.dart';

import 'feedback.dart';
import 'skeleton.dart';
import '../core/api_client.dart';

/// Loads data with [load], shows loading / error-with-retry states, and supports pull-to-refresh.
/// Call the `reload` passed to [builder] after a mutation.
class AsyncView<T> extends StatefulWidget {
  const AsyncView(
      {super.key,
      required this.load,
      required this.builder,
      this.skeleton = const SkeletonDetail()});

  /// Shown while the data loads, shaped like the screen that follows.
  final Widget skeleton;
  final Future<T> Function() load;
  final Widget Function(
      BuildContext context, T data, Future<void> Function() reload) builder;

  @override
  State<AsyncView<T>> createState() => _AsyncViewState<T>();
}

class _AsyncViewState<T> extends State<AsyncView<T>> {
  T? _data;
  Object? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await widget.load();
      if (mounted) {
        setState(() {
          _data = data;
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e;
          _loading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading && _data == null) {
      return widget.skeleton;
    }
    if (_error != null && _data == null) {
      return ListView(
          padding: const EdgeInsets.all(16),
          children: [ErrorBanner(errorMessage(_error!), onRetry: _reload)]);
    }
    return RefreshIndicator(
        onRefresh: _reload,
        child: widget.builder(context, _data as T, _reload));
  }
}
