import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/auth/user_role.dart';
import '../cubit/login_cubit.dart';
import '../cubit/login_state.dart';

class RoleLoginPage extends StatelessWidget {
  const RoleLoginPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocListener<LoginCubit, LoginState>(
      listenWhen:
          (previous, current) =>
              previous.status != current.status &&
              current.status == LoginStatus.failure,
      listener: (context, state) {
        final errorMessage = state.errorMessage;
        if (errorMessage == null) {
          return;
        }

        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(errorMessage)));
      },
      child: Scaffold(
        body: SafeArea(
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: BlocBuilder<LoginCubit, LoginState>(
                  builder: (context, state) {
                    return Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(
                          'Foodya',
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.displaySmall,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Select your role to continue',
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.bodyLarge,
                        ),
                        const SizedBox(height: 32),
                        FilledButton.icon(
                          onPressed:
                              state.isSubmitting
                                  ? null
                                  : () => context.read<LoginCubit>().loginAs(
                                    UserRole.customer,
                                  ),
                          icon: const Icon(Icons.person_outline),
                          label: const Text('Continue as Customer'),
                        ),
                        const SizedBox(height: 12),
                        FilledButton.tonalIcon(
                          onPressed:
                              state.isSubmitting
                                  ? null
                                  : () => context.read<LoginCubit>().loginAs(
                                    UserRole.merchant,
                                  ),
                          icon: const Icon(Icons.storefront_outlined),
                          label: const Text('Continue as Merchant'),
                        ),
                        const SizedBox(height: 20),
                        if (state.isSubmitting)
                          const Center(
                            child: SizedBox(
                              height: 22,
                              width: 22,
                              child: CircularProgressIndicator(
                                strokeWidth: 2.5,
                              ),
                            ),
                          ),
                      ],
                    );
                  },
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
