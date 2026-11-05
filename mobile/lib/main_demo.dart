import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import 'app.dart';
import 'core/auth/session_cubit.dart';
import 'core/auth/user_role.dart';
import 'core/router/app_router.dart';
import 'features/auth/data/repositories/mock_auth_repository.dart';
import 'features/auth/domain/repositories/auth_repository.dart';
import 'features/auth/presentation/cubit/login_cubit.dart';
import 'features/delivery/data/repositories/demo_delivery_repository.dart';
import 'features/delivery/domain/repositories/delivery_repository.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const FoodyaDeliveryDemo());
}

class FoodyaDeliveryDemo extends StatefulWidget {
  const FoodyaDeliveryDemo({super.key});

  @override
  State<FoodyaDeliveryDemo> createState() => _FoodyaDeliveryDemoState();
}

class _FoodyaDeliveryDemoState extends State<FoodyaDeliveryDemo> {
  static const _configuredScenario = String.fromEnvironment(
    'FOODYA_DEMO_SCENARIO',
    defaultValue: 'single-assignment',
  );

  late final SessionCubit _sessionCubit;
  late final AuthRepository _authRepository;
  late final GoRouter _router;
  late final DemoDeliveryRepository _repository;
  late DeliveryDemoScenario _scenario;

  @override
  void initState() {
    super.initState();
    _scenario = DeliveryDemoScenario.fromKey(
      Uri.base.queryParameters['scenario'] ?? _configuredScenario,
    );
    _sessionCubit = SessionCubit()..signInAs(UserRole.delivery);
    _authRepository = MockAuthRepository();
    _repository = DemoDeliveryRepository(scenario: _scenario);
    _router =
        AppRouter(
          _sessionCubit,
          initialLocation: _pathForScenario(_scenario),
        ).router;
  }

  @override
  void dispose() {
    _router.dispose();
    _sessionCubit.close();
    super.dispose();
  }

  void _selectScenario(DeliveryDemoScenario scenario) {
    if (scenario == _scenario) return;
    setState(() {
      _scenario = scenario;
      _repository.setScenario(scenario);
    });
    _goToScenario(scenario);
  }

  void _goToScenario(DeliveryDemoScenario scenario) {
    _router.go(_pathForScenario(scenario));
  }

  String _pathForScenario(DeliveryDemoScenario scenario) {
    return switch (scenario) {
      DeliveryDemoScenario.batchDetail => '/delivery/batches/active',
      DeliveryDemoScenario.batchProgress =>
        '/delivery/batches/BATCH-DEMO-2404/manage',
      DeliveryDemoScenario.walletPreview => '/delivery/wallet',
      _ => '/delivery/home',
    };
  }

  @override
  Widget build(BuildContext context) {
    return MultiRepositoryProvider(
      key: ValueKey(_scenario),
      providers: [
        RepositoryProvider<AuthRepository>.value(value: _authRepository),
        RepositoryProvider<DeliveryRepository>.value(value: _repository),
      ],
      child: MultiBlocProvider(
        providers: [
          BlocProvider<SessionCubit>.value(value: _sessionCubit),
          BlocProvider<LoginCubit>(
            create:
                (_) => LoginCubit(
                  authRepository: _authRepository,
                  sessionCubit: _sessionCubit,
                ),
          ),
        ],
        child: FoodyaMobileApp(
          router: _router,
          builder:
              (context, child) => Stack(
                children: [
                  if (child != null) child,
                  Positioned(
                    top: MediaQuery.paddingOf(context).top + 6,
                    right: 56,
                    child: Material(
                      color: Colors.transparent,
                      child: PopupMenuButton<DeliveryDemoScenario>(
                        tooltip: 'Choose demo scenario',
                        onSelected: _selectScenario,
                        itemBuilder:
                            (context) => [
                              for (final scenario
                                  in DeliveryDemoScenario.values)
                                PopupMenuItem(
                                  value: scenario,
                                  child: Text(scenario.key),
                                ),
                            ],
                        child: Chip(
                          avatar: const Icon(Icons.science_outlined, size: 18),
                          label: Text(_scenario.key),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
        ),
      ),
    );
  }
}
