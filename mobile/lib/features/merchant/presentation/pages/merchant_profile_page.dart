import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/ui/foodya_ui.dart';
import '../../../customer/domain/models/user_profile.dart';
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

  @override
  void dispose() {
    _fullNameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
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
          appBar: AppBar(
            title: const Text('Merchant Profile'),
            actions: [
              IconButton(
                icon: const Icon(Icons.refresh_outlined),
                tooltip: 'Refresh',
                onPressed:
                    state.isBusy
                        ? null
                        : () => context.read<ProfileCubit>().load(),
              ),
            ],
          ),
          body:
              state.status == ProfileStatus.loading && state.profile == null
                  ? const Center(child: CircularProgressIndicator())
                  : RefreshIndicator(
                    onRefresh: () => context.read<ProfileCubit>().load(),
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        _ProfileHeader(profile: state.profile),
                        const SizedBox(height: 20),
                        Form(
                          key: _formKey,
                          child: Card(
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    'Personal info',
                                    style: Theme.of(
                                      context,
                                    ).textTheme.labelLarge?.copyWith(
                                      color:
                                          Theme.of(context).colorScheme.primary,
                                    ),
                                  ),
                                  const SizedBox(height: 16),
                                  TextFormField(
                                    controller: _fullNameController,
                                    textCapitalization:
                                        TextCapitalization.words,
                                    decoration: const InputDecoration(
                                      labelText: 'Full name',
                                      prefixIcon: Icon(Icons.person_outline),
                                    ),
                                    validator:
                                        (value) =>
                                            value == null ||
                                                    value.trim().isEmpty
                                                ? 'Full name is required.'
                                                : null,
                                  ),
                                  const SizedBox(height: 12),
                                  TextFormField(
                                    controller: _emailController,
                                    decoration: const InputDecoration(
                                      labelText: 'Email',
                                      prefixIcon: Icon(Icons.email_outlined),
                                    ),
                                    keyboardType: TextInputType.emailAddress,
                                    validator:
                                        (value) =>
                                            value == null ||
                                                    !value.contains('@')
                                                ? 'Valid email is required.'
                                                : null,
                                  ),
                                  const SizedBox(height: 12),
                                  TextFormField(
                                    controller: _phoneController,
                                    decoration: const InputDecoration(
                                      labelText: 'Phone number',
                                      prefixIcon: Icon(Icons.phone_outlined),
                                    ),
                                    keyboardType: TextInputType.phone,
                                    validator:
                                        (value) =>
                                            value == null ||
                                                    value.trim().isEmpty
                                                ? 'Phone number is required.'
                                                : null,
                                  ),
                                  const SizedBox(height: 12),
                                  ImagePickerField(
                                    label: 'Profile photo',
                                    pickedFile: state.avatarImageFile,
                                    currentUrl: state.profile?.avatarUrl,
                                    onChanged: (file) {
                                      if (file != null) {
                                        context
                                            .read<ProfileCubit>()
                                            .uploadAvatar(file);
                                      }
                                    },
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton.icon(
                            onPressed:
                                state.isBusy
                                    ? null
                                    : () => _saveProfile(
                                      context,
                                      avatarUrl: state.profile?.avatarUrl,
                                    ),
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
                        ),
                        const SizedBox(height: 8),
                        SizedBox(
                          width: double.infinity,
                          child: OutlinedButton.icon(
                            onPressed:
                                state.isBusy
                                    ? null
                                    : () => _showPasswordDialog(context),
                            icon: const Icon(Icons.lock_reset_outlined),
                            label: const Text('Change password'),
                          ),
                        ),
                      ],
                    ),
                  ),
        );
      },
    );
  }

  void _saveProfile(BuildContext context, {String? avatarUrl}) {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    context.read<ProfileCubit>().save(
      fullName: _fullNameController.text.trim(),
      email: _emailController.text.trim(),
      phoneNumber: _phoneController.text.trim(),
      avatarUrl: avatarUrl,
    );
  }

  void _showPasswordDialog(BuildContext context) {
    final currentController = TextEditingController();
    final nextController = TextEditingController();
    final confirmController = TextEditingController();
    final formKey = GlobalKey<FormState>();
    var showCurrent = false;
    var showNext = false;
    var showConfirm = false;

    showDialog<void>(
      context: context,
      builder:
          (dialogContext) => StatefulBuilder(
            builder:
                (_, setDialogState) => AlertDialog(
                  title: const Text('Change password'),
                  content: Form(
                    key: formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        TextFormField(
                          controller: currentController,
                          obscureText: !showCurrent,
                          decoration: InputDecoration(
                            labelText: 'Current password',
                            suffixIcon: IconButton(
                              icon: Icon(
                                showCurrent
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                              ),
                              onPressed:
                                  () => setDialogState(
                                    () => showCurrent = !showCurrent,
                                  ),
                            ),
                          ),
                          validator:
                              (v) =>
                                  v == null || v.isEmpty ? 'Required.' : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: nextController,
                          obscureText: !showNext,
                          decoration: InputDecoration(
                            labelText: 'New password',
                            suffixIcon: IconButton(
                              icon: Icon(
                                showNext
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                              ),
                              onPressed:
                                  () => setDialogState(
                                    () => showNext = !showNext,
                                  ),
                            ),
                          ),
                          validator:
                              (v) =>
                                  v == null || v.length < 6
                                      ? 'At least 6 characters.'
                                      : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: confirmController,
                          obscureText: !showConfirm,
                          decoration: InputDecoration(
                            labelText: 'Confirm password',
                            suffixIcon: IconButton(
                              icon: Icon(
                                showConfirm
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                              ),
                              onPressed:
                                  () => setDialogState(
                                    () => showConfirm = !showConfirm,
                                  ),
                            ),
                          ),
                          validator:
                              (v) =>
                                  v != nextController.text
                                      ? 'Passwords do not match.'
                                      : null,
                        ),
                      ],
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.of(dialogContext).pop(),
                      child: const Text('Cancel'),
                    ),
                    FilledButton(
                      onPressed: () {
                        if (!formKey.currentState!.validate()) return;
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
          ),
    ).then((_) {
      currentController.dispose();
      nextController.dispose();
      confirmController.dispose();
    });
  }
}

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader({required this.profile});

  final UserProfile? profile;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final name = profile?.fullName ?? '';
    final initials = name
        .trim()
        .split(' ')
        .where((w) => w.isNotEmpty)
        .take(2)
        .map((w) => w[0].toUpperCase())
        .join();
    final avatarUrl = profile?.avatarUrl?.trim();
    final hasAvatar = avatarUrl != null && avatarUrl.isNotEmpty;

    return Column(
      children: [
        CircleAvatar(
          radius: 48,
          backgroundColor: theme.colorScheme.primaryContainer,
          backgroundImage: hasAvatar ? NetworkImage(avatarUrl) : null,
          child:
              hasAvatar
                  ? null
                  : Text(
                    initials.isEmpty ? '?' : initials,
                    style: theme.textTheme.headlineMedium?.copyWith(
                      color: theme.colorScheme.onPrimaryContainer,
                    ),
                  ),
        ),
        const SizedBox(height: 12),
        Text(
          name.isEmpty ? 'Your Profile' : name,
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w700,
          ),
          textAlign: TextAlign.center,
        ),
        if (profile != null) ...[
          const SizedBox(height: 4),
          Text(
            profile!.email,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          if (profile!.role.isNotEmpty) ...[
            const SizedBox(height: 8),
            Chip(
              label: Text(profile!.role),
              labelStyle: theme.textTheme.labelSmall,
              padding: EdgeInsets.zero,
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
          ],
        ],
      ],
    );
  }
}
