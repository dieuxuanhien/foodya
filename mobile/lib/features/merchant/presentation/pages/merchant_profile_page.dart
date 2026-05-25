import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../customer/domain/repositories/customer_profile_repository.dart';
import '../../../customer/presentation/cubit/profile_cubit.dart';
import '../../../customer/presentation/cubit/profile_state.dart';

class MerchantProfilePage extends StatelessWidget {
  const MerchantProfilePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => ProfileCubit(
            repository: context.read<CustomerProfileRepository>(),
          )..load(),
      child: const _MerchantProfileView(),
    );
  }
}

class _MerchantProfileView extends StatefulWidget {
  const _MerchantProfileView();

  @override
  State<_MerchantProfileView> createState() => _MerchantProfileViewState();
}

class _MerchantProfileViewState extends State<_MerchantProfileView> {
  final _formKey = GlobalKey<FormState>();
  final _fullNameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _avatarController = TextEditingController();

  @override
  void dispose() {
    _fullNameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _avatarController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<ProfileCubit, ProfileState>(
      listenWhen:
          (previous, current) =>
              previous.profile != current.profile ||
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final profile = state.profile;
        if (profile != null) {
          _fullNameController.text = profile.fullName;
          _emailController.text = profile.email;
          _phoneController.text = profile.phoneNumber;
          _avatarController.text = profile.avatarUrl ?? '';
        }

        final message = state.errorMessage ?? state.infoMessage;
        if (message != null) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(message)));
          context.read<ProfileCubit>().clearFeedback();
        }
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(title: const Text('Merchant Profile')),
          body:
              state.status == ProfileStatus.loading && state.profile == null
                  ? const Center(child: CircularProgressIndicator())
                  : RefreshIndicator(
                    onRefresh: () => context.read<ProfileCubit>().load(),
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        Form(
                          key: _formKey,
                          child: Column(
                            children: [
                              TextFormField(
                                controller: _fullNameController,
                                decoration: const InputDecoration(
                                  labelText: 'Full name',
                                ),
                                validator:
                                    (value) =>
                                        value == null || value.trim().isEmpty
                                            ? 'Full name is required.'
                                            : null,
                              ),
                              const SizedBox(height: 12),
                              TextFormField(
                                controller: _emailController,
                                decoration: const InputDecoration(
                                  labelText: 'Email',
                                ),
                                keyboardType: TextInputType.emailAddress,
                                validator:
                                    (value) =>
                                        value == null || !value.contains('@')
                                            ? 'Valid email is required.'
                                            : null,
                              ),
                              const SizedBox(height: 12),
                              TextFormField(
                                controller: _phoneController,
                                decoration: const InputDecoration(
                                  labelText: 'Phone number',
                                ),
                                keyboardType: TextInputType.phone,
                                validator:
                                    (value) =>
                                        value == null || value.trim().isEmpty
                                            ? 'Phone number is required.'
                                            : null,
                              ),
                              const SizedBox(height: 12),
                              TextFormField(
                                controller: _avatarController,
                                decoration: const InputDecoration(
                                  labelText: 'Avatar URL',
                                ),
                              ),
                              const SizedBox(height: 16),
                              FilledButton.icon(
                                onPressed:
                                    state.isBusy
                                        ? null
                                        : () => _saveProfile(context),
                                icon:
                                    state.status == ProfileStatus.saving
                                        ? const SizedBox(
                                          width: 18,
                                          height: 18,
                                          child: CircularProgressIndicator(
                                            strokeWidth: 2,
                                          ),
                                        )
                                        : const Icon(Icons.save_outlined),
                                label: const Text('Save profile'),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 24),
                        OutlinedButton.icon(
                          onPressed:
                              state.isBusy
                                  ? null
                                  : () => _showPasswordDialog(context),
                          icon: const Icon(Icons.lock_reset_outlined),
                          label: const Text('Change password'),
                        ),
                      ],
                    ),
                  ),
        );
      },
    );
  }

  void _saveProfile(BuildContext context) {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    context.read<ProfileCubit>().save(
      fullName: _fullNameController.text,
      email: _emailController.text,
      phoneNumber: _phoneController.text,
      avatarUrl: _avatarController.text,
    );
  }

  void _showPasswordDialog(BuildContext context) {
    final currentController = TextEditingController();
    final nextController = TextEditingController();
    final confirmController = TextEditingController();

    showDialog<void>(
      context: context,
      builder:
          (dialogContext) => AlertDialog(
            title: const Text('Change password'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: currentController,
                  obscureText: true,
                  decoration: const InputDecoration(
                    labelText: 'Current password',
                  ),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: nextController,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: 'New password'),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: confirmController,
                  obscureText: true,
                  decoration: const InputDecoration(
                    labelText: 'Confirm password',
                  ),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () {
                  Navigator.of(dialogContext).pop();
                  context.read<ProfileCubit>().changePassword(
                    currentPassword: currentController.text,
                    newPassword: nextController.text,
                    confirmPassword: confirmController.text,
                  );
                },
                child: const Text('Update'),
              ),
            ],
          ),
    );
  }
}
